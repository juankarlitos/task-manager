package com.taskmanager.controller;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // POST /api/tasks — Crear tarea
    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request));
    }

    // GET /api/tasks — Listar todas las tareas
    @GetMapping
    public ResponseEntity<List<TaskResponse>> findAll() {
        return ResponseEntity.ok(taskService.findAll());
    }

    // GET /api/tasks/{id} — Obtener tarea por ID
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.findById(id));
    }

    // GET /api/tasks/status/{status} — Filtrar por estado
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TaskResponse>> findByStatus(@PathVariable TaskStatus status) {
        return ResponseEntity.ok(taskService.findByStatus(status));
    }

    // GET /api/tasks/priority/{priority} — Filtrar por prioridad
    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<TaskResponse>> findByPriority(@PathVariable TaskPriority priority) {
        return ResponseEntity.ok(taskService.findByPriority(priority));
    }

    // GET /api/tasks/assigned/{name} — Filtrar por responsable
    @GetMapping("/assigned/{name}")
    public ResponseEntity<List<TaskResponse>> findByAssignedTo(@PathVariable String name) {
        return ResponseEntity.ok(taskService.findByAssignedTo(name));
    }

    // GET /api/tasks/search?q=keyword — Búsqueda libre
    @GetMapping("/search")
    public ResponseEntity<List<TaskResponse>> search(@RequestParam("q") String keyword) {
        return ResponseEntity.ok(taskService.search(keyword));
    }

    // GET /api/tasks/stats — Estadísticas por estado
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats() {
        return ResponseEntity.ok(taskService.getStatsByStatus());
    }

    // PUT /api/tasks/{id} — Actualizar tarea completa
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    // PATCH /api/tasks/{id}/status — Cambiar solo el estado
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam TaskStatus status) {
        return ResponseEntity.ok(taskService.updateStatus(id, status));
    }

    // DELETE /api/tasks/{id} — Eliminar tarea
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
