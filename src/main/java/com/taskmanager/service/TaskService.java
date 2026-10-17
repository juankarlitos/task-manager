package com.taskmanager.service;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;

import java.util.List;
import java.util.Map;

public interface TaskService {

    TaskResponse create(TaskRequest request, String username);

    TaskResponse findById(Long id, String username);

    List<TaskResponse> findAll(String username);

    List<TaskResponse> findByStatus(TaskStatus status, String username);

    List<TaskResponse> findByPriority(TaskPriority priority, String username);

    List<TaskResponse> findByAssignedTo(String assignedTo, String username);

    List<TaskResponse> search(String keyword, String username);

    TaskResponse update(Long id, TaskRequest request, String username);

    TaskResponse updateStatus(Long id, TaskStatus newStatus, String username);

    void delete(Long id, String username);

    Map<String, Long> getStatsByStatus(String username);
}
