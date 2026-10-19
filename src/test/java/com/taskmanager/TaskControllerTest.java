package com.taskmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.dto.AuthRequest;
import com.taskmanager.dto.TaskRequest;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.model.User;
import com.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;

    @BeforeAll
    void setup() throws Exception {
        if (!userRepository.existsByUsername("testuser")) {
            userRepository.save(User.builder()
                    .username("testuser")
                    .password(passwordEncoder.encode("test123"))
                    .role("ROLE_USER")
                    .build());
        }

        AuthRequest loginReq = new AuthRequest();
        loginReq.setUsername("testuser");
        loginReq.setPassword("test123");

        String resp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        authToken = objectMapper.readTree(resp).get("token").asText();
    }

    private MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder req) {
        return req.header("Authorization", "Bearer " + authToken);
    }

    @Test
    void shouldReturnAllTasks() throws Exception {
        mockMvc.perform(withAuth(get("/api/tasks")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldCreateTask() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("Nueva tarea de prueba");
        request.setDescription("Descripción de prueba");
        request.setStatus(TaskStatus.PENDING);
        request.setPriority(TaskPriority.MEDIUM);
        request.setAssignedTo("Tester");
        request.setDueDate(LocalDateTime.now().plusDays(7));

        mockMvc.perform(withAuth(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Nueva tarea de prueba"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.ownerUsername").value("testuser"));
    }

    @Test
    void shouldReturnNotFoundForInvalidId() throws Exception {
        mockMvc.perform(withAuth(get("/api/tasks/9999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldFailValidationWithBlankTitle() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("  ");
        request.setStatus(TaskStatus.PENDING);
        request.setPriority(TaskPriority.LOW);

        mockMvc.perform(withAuth(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void shouldReturnStats() throws Exception {
        mockMvc.perform(withAuth(get("/api/tasks/stats")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.PENDING").exists())
                .andExpect(jsonPath("$.COMPLETED").exists());
    }

    @Test
    void shouldUpdateTaskStatus() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("Tarea para actualizar estado");
        request.setStatus(TaskStatus.PENDING);
        request.setPriority(TaskPriority.LOW);
        request.setDueDate(LocalDateTime.now().plusDays(5));

        String response = mockMvc.perform(withAuth(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(withAuth(patch("/api/tasks/" + id + "/status")
                        .param("status", "IN_PROGRESS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void cuandoCambiaContrasena_tokenAntiguoEsRechazado() throws Exception {
        // Crear usuario exclusivo para este test
        if (!userRepository.existsByUsername("pwdresetuser")) {
            userRepository.save(User.builder()
                    .username("pwdresetuser")
                    .password(passwordEncoder.encode("reset123"))
                    .role("ROLE_USER")
                    .build());
        }

        AuthRequest loginReq = new AuthRequest();
        loginReq.setUsername("pwdresetuser");
        loginReq.setPassword("reset123");

        String resp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String oldToken = objectMapper.readTree(resp).get("token").asText();

        // Verificar que el token funciona antes del cambio
        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isOk());

        // Simular cambio de contraseña: marcar passwordChangedAt en el futuro respecto al iat
        User user = userRepository.findByUsername("pwdresetuser").orElseThrow();
        user.setPasswordChangedAt(LocalDateTime.now().plusSeconds(5));
        userRepository.save(user);

        // El token antiguo debe ser rechazado
        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotSeeAnotherUsersTasks() throws Exception {
        // Crear usuario ajeno
        if (!userRepository.existsByUsername("otheruser")) {
            userRepository.save(User.builder()
                    .username("otheruser")
                    .password(passwordEncoder.encode("other123"))
                    .role("ROLE_USER")
                    .build());
        }

        // Login como otheruser
        AuthRequest loginReq = new AuthRequest();
        loginReq.setUsername("otheruser");
        loginReq.setPassword("other123");

        String resp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String otherToken = objectMapper.readTree(resp).get("token").asText();

        // Crear una tarea como testuser
        TaskRequest req = new TaskRequest();
        req.setTitle("Tarea privada de testuser");
        req.setStatus(TaskStatus.PENDING);
        req.setPriority(TaskPriority.LOW);

        String taskResp = mockMvc.perform(withAuth(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(taskResp).get("id").asLong();

        // otheruser intenta acceder → debe obtener 404
        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }
}
