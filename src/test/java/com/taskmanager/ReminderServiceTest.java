package com.taskmanager;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.model.User;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.service.EmailService;
import com.taskmanager.service.ReminderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ReminderServiceImpl reminderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reminderService, "leadMinutes", 30);
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    private User userWithEmail(String email) {
        return User.builder()
                .id(1L)
                .username("testuser")
                .email(email)
                .password("pwd")
                .role("ROLE_USER")
                .build();
    }

    private Task pendingTask(User owner) {
        return Task.builder()
                .id(10L)
                .title("Tarea de prueba")
                .description("Descripción")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDateTime.now().minusMinutes(5))
                .owner(owner)
                .build();
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void cuandoHayTareaElegible_enviaCorreoYMarcaComoEnviado() {
        Task task = pendingTask(userWithEmail("user@example.com"));
        when(taskRepository.findTasksDueForReminder(any(LocalDateTime.class), anyList()))
                .thenReturn(List.of(task));

        int sent = reminderService.sendReminders();

        assertThat(sent).isEqualTo(1);
        verify(emailService).sendReminderEmail(task);
        assertThat(task.getReminderSent()).isTrue();

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getReminderSent()).isTrue();
    }

    @Test
    void cuandoFallaElEnvio_noMarcaComoEnviadoYContinua() {
        Task task = pendingTask(userWithEmail("user@example.com"));
        when(taskRepository.findTasksDueForReminder(any(LocalDateTime.class), anyList()))
                .thenReturn(List.of(task));
        doThrow(new RuntimeException("Brevo no disponible"))
                .when(emailService).sendReminderEmail(any());

        int sent = reminderService.sendReminders();

        assertThat(sent).isEqualTo(0);
        verify(taskRepository, never()).save(any());
        assertThat(task.getReminderSent()).isFalse();
    }

    @Test
    void cuandoNoHayTareas_devuelveCero() {
        when(taskRepository.findTasksDueForReminder(any(LocalDateTime.class), anyList()))
                .thenReturn(Collections.emptyList());

        int sent = reminderService.sendReminders();

        assertThat(sent).isEqualTo(0);
        verifyNoInteractions(emailService);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void conVariasTareas_procesaTodasYCuentaSoloExitosas() {
        User owner = userWithEmail("user@example.com");
        Task taskOk   = pendingTask(owner);
        Task taskFail = Task.builder()
                .id(11L).title("Tarea que falla").description("desc")
                .status(TaskStatus.IN_PROGRESS).priority(TaskPriority.LOW)
                .dueDate(LocalDateTime.now()).owner(owner).build();

        when(taskRepository.findTasksDueForReminder(any(LocalDateTime.class), anyList()))
                .thenReturn(List.of(taskOk, taskFail));
        doNothing().when(emailService).sendReminderEmail(taskOk);
        doThrow(new RuntimeException("Error red")).when(emailService).sendReminderEmail(taskFail);

        int sent = reminderService.sendReminders();

        assertThat(sent).isEqualTo(1);
        verify(emailService).sendReminderEmail(taskOk);
        verify(emailService).sendReminderEmail(taskFail);
        verify(taskRepository, times(1)).save(any());
        assertThat(taskOk.getReminderSent()).isTrue();
        assertThat(taskFail.getReminderSent()).isFalse();
    }

    @Test
    void elDeadlineIncluye_leadMinutesDesdeAhora() {
        when(taskRepository.findTasksDueForReminder(any(LocalDateTime.class), anyList()))
                .thenReturn(Collections.emptyList());

        LocalDateTime antes = LocalDateTime.now().plusMinutes(30).minusSeconds(2);
        reminderService.sendReminders();
        LocalDateTime despues = LocalDateTime.now().plusMinutes(30).plusSeconds(2);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(taskRepository).findTasksDueForReminder(captor.capture(), anyList());

        LocalDateTime deadline = captor.getValue();
        assertThat(deadline).isAfter(antes).isBefore(despues);
    }
}
