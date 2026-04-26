package com.taskmanager.service;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.exception.TaskNotFoundException;
import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    @Override
    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus())
                .priority(request.getPriority())
                .assignedTo(request.getAssignedTo())
                .dueDate(request.getDueDate())
                .build();
        return toResponse(taskRepository.save(task));
    }

    @Override
    public TaskResponse findById(Long id) {
        return taskRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Override
    public List<TaskResponse> findAll() {
        return taskRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> findByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> findByPriority(TaskPriority priority) {
        return taskRepository.findByPriority(priority).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> findByAssignedTo(String assignedTo) {
        return taskRepository.findByAssignedToIgnoreCase(assignedTo).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> search(String keyword) {
        return taskRepository.searchByKeyword(keyword).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setAssignedTo(request.getAssignedTo());
        task.setDueDate(request.getDueDate());

        return toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional
    public TaskResponse updateStatus(Long id, TaskStatus newStatus) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        task.setStatus(newStatus);
        return toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteById(id);
    }

    @Override
    public Map<String, Long> getStatsByStatus() {
        Map<String, Long> stats = new LinkedHashMap<>();
        Arrays.stream(TaskStatus.values()).forEach(status ->
                stats.put(status.name(), taskRepository.countByStatus(status))
        );
        return stats;
    }

    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .assignedTo(task.getAssignedTo())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
