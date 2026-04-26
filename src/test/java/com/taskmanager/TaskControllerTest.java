package com.taskmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.dto.TaskRequest;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnAllTasks() throws Exception {
        mockMvc.perform(get("/api/tasks"))
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

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Nueva tarea de prueba"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturnNotFoundForInvalidId() throws Exception {
        mockMvc.perform(get("/api/tasks/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldFailValidationWithBlankTitle() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("  ");
        request.setStatus(TaskStatus.PENDING);
        request.setPriority(TaskPriority.LOW);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void shouldReturnStats() throws Exception {
        mockMvc.perform(get("/api/tasks/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.PENDING").exists())
                .andExpect(jsonPath("$.COMPLETED").exists());
    }

    @Test
    void shouldUpdateTaskStatus() throws Exception {
        // Primero crear una tarea
        TaskRequest request = new TaskRequest();
        request.setTitle("Tarea para actualizar estado");
        request.setStatus(TaskStatus.PENDING);
        request.setPriority(TaskPriority.LOW);
        request.setDueDate(LocalDateTime.now().plusDays(5));

        String response = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        // Luego actualizar el estado
        mockMvc.perform(patch("/api/tasks/" + id + "/status")
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
}
