package com.taskmanager.dto;

import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(min = 3, max = 150, message = "El título debe tener entre 3 y 150 caracteres")
    private String title;

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    private String description;

    @NotNull(message = "El estado es obligatorio")
    private TaskStatus status;

    @NotNull(message = "La prioridad es obligatoria")
    private TaskPriority priority;

    @Size(max = 100, message = "El nombre del asignado no puede superar 100 caracteres")
    private String assignedTo;

    @Future(message = "La fecha de vencimiento debe ser en el futuro")
    private LocalDateTime dueDate;
}
