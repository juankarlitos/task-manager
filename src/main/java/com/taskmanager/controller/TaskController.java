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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> create(
            @Valid @RequestBody TaskRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request, auth.getName()));
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> findAll(Authentication auth) {
        return ResponseEntity.ok(taskService.findAll(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> findById(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(taskService.findById(id, auth.getName()));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TaskResponse>> findByStatus(
            @PathVariable TaskStatus status, Authentication auth) {
        return ResponseEntity.ok(taskService.findByStatus(status, auth.getName()));
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<TaskResponse>> findByPriority(
            @PathVariable TaskPriority priority, Authentication auth) {
        return ResponseEntity.ok(taskService.findByPriority(priority, auth.getName()));
    }

    @GetMapping("/assigned/{name}")
    public ResponseEntity<List<TaskResponse>> findByAssignedTo(
            @PathVariable String name, Authentication auth) {
        return ResponseEntity.ok(taskService.findByAssignedTo(name, auth.getName()));
    }

    @GetMapping("/search")
    public ResponseEntity<List<TaskResponse>> search(
            @RequestParam("q") String keyword, Authentication auth) {
        return ResponseEntity.ok(taskService.search(keyword, auth.getName()));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats(Authentication auth) {
        return ResponseEntity.ok(taskService.getStatsByStatus(auth.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            Authentication auth) {
        return ResponseEntity.ok(taskService.update(id, request, auth.getName()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam TaskStatus status,
            Authentication auth) {
        return ResponseEntity.ok(taskService.updateStatus(id, status, auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        taskService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
