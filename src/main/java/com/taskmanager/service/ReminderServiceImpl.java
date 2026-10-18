package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderServiceImpl implements ReminderService {

    private static final List<TaskStatus> EXCLUDED_STATUSES =
            List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED);

    private final TaskRepository taskRepository;
    private final EmailService emailService;

    @Value("${app.reminders.lead-minutes:30}")
    private int leadMinutes;

    @Override
    public int sendReminders() {
        LocalDateTime deadline = LocalDateTime.now().plusMinutes(leadMinutes);
        List<Task> candidates = taskRepository.findTasksDueForReminder(deadline, EXCLUDED_STATUSES);

        log.info("Procesando recordatorios: {} tarea(s) candidata(s) (ventana: {} min)",
                candidates.size(), leadMinutes);

        int sent = 0;
        for (Task task : candidates) {
            try {
                emailService.sendReminderEmail(task);
                task.setReminderSent(Boolean.TRUE);
                taskRepository.save(task);
                sent++;
            } catch (Exception e) {
                log.error("Error al enviar recordatorio para tarea {} ('{}'): {}",
                        task.getId(), task.getTitle(), e.getMessage());
            }
        }

        log.info("Recordatorios completados: {}/{} enviados", sent, candidates.size());
        return sent;
    }
}
