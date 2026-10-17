package com.taskmanager.service;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.exception.TaskNotFoundException;
import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.model.User;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TaskResponse create(TaskRequest request, String username) {
        User owner = getUser(username);
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus())
                .priority(request.getPriority())
                .assignedTo(request.getAssignedTo())
                .dueDate(request.getDueDate())
                .owner(owner)
                .build();
        return toResponse(taskRepository.save(task));
    }

    @Override
    public TaskResponse findById(Long id, String username) {
        User owner = getUser(username);
        return taskRepository.findByIdAndOwner(id, owner)
                .map(this::toResponse)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Override
    public List<TaskResponse> findAll(String username) {
        User owner = getUser(username);
        return taskRepository.findByOwner(owner).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> findByStatus(TaskStatus status, String username) {
        User owner = getUser(username);
        return taskRepository.findByStatusAndOwner(status, owner).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> findByPriority(TaskPriority priority, String username) {
        User owner = getUser(username);
        return taskRepository.findByPriorityAndOwner(priority, owner).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> findByAssignedTo(String assignedTo, String username) {
        User owner = getUser(username);
        return taskRepository.findByAssignedToIgnoreCaseAndOwner(assignedTo, owner).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> search(String keyword, String username) {
        User owner = getUser(username);
        return taskRepository.searchByKeywordAndOwner(keyword, owner).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskResponse update(Long id, TaskRequest request, String username) {
        User owner = getUser(username);
        Task task = taskRepository.findByIdAndOwner(id, owner)
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
    public TaskResponse updateStatus(Long id, TaskStatus newStatus, String username) {
        User owner = getUser(username);
        Task task = taskRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new TaskNotFoundException(id));
        task.setStatus(newStatus);
        return toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional
    public void delete(Long id, String username) {
        User owner = getUser(username);
        if (!taskRepository.findByIdAndOwner(id, owner).isPresent()) {
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteById(id);
    }

    @Override
    public Map<String, Long> getStatsByStatus(String username) {
        User owner = getUser(username);
        Map<String, Long> stats = new LinkedHashMap<>();
        Arrays.stream(TaskStatus.values()).forEach(status ->
                stats.put(status.name(), taskRepository.countByStatusAndOwner(status, owner))
        );
        return stats;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado: " + username));
    }

    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .assignedTo(task.getAssignedTo())
                .ownerUsername(task.getOwner() != null ? task.getOwner().getUsername() : null)
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
