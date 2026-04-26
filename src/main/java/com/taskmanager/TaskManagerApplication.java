package com.taskmanager;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.repository.TaskRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

@SpringBootApplication
public class TaskManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskManagerApplication.class, args);
    }

    // Datos de ejemplo al iniciar
    @Bean
    CommandLineRunner seedData(TaskRepository repo) {
        return args -> {
            repo.save(Task.builder()
                    .title("Diseñar base de datos")
                    .description("Crear el modelo entidad-relación para el módulo de usuarios")
                    .status(TaskStatus.COMPLETED)
                    .priority(TaskPriority.HIGH)
                    .assignedTo("Ana García")
                    .dueDate(LocalDateTime.now().plusDays(5))
                    .build());

            repo.save(Task.builder()
                    .title("Implementar autenticación JWT")
                    .description("Integrar Spring Security con tokens JWT para la API REST")
                    .status(TaskStatus.IN_PROGRESS)
                    .priority(TaskPriority.CRITICAL)
                    .assignedTo("Carlos López")
                    .dueDate(LocalDateTime.now().plusDays(3))
                    .build());

            repo.save(Task.builder()
                    .title("Escribir pruebas unitarias")
                    .description("Cobertura mínima del 80% para los servicios principales")
                    .status(TaskStatus.PENDING)
                    .priority(TaskPriority.MEDIUM)
                    .assignedTo("María Torres")
                    .dueDate(LocalDateTime.now().plusDays(10))
                    .build());

            repo.save(Task.builder()
                    .title("Configurar CI/CD en GitHub Actions")
                    .description("Pipeline de build, test y deploy automático")
                    .status(TaskStatus.PENDING)
                    .priority(TaskPriority.LOW)
                    .assignedTo("Carlos López")
                    .dueDate(LocalDateTime.now().plusDays(15))
                    .build());

            repo.save(Task.builder()
                    .title("Revisión de seguridad")
                    .description("Auditoría de vulnerabilidades OWASP Top 10")
                    .status(TaskStatus.CANCELLED)
                    .priority(TaskPriority.HIGH)
                    .assignedTo("Ana García")
                    .build());

            System.out.println("\n>>> Datos de ejemplo cargados correctamente <<<\n");
        };
    }
}
