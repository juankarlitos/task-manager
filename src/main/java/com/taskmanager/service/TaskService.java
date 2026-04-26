package com.taskmanager.service;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;

import java.util.List;
import java.util.Map;

public interface TaskService {

    TaskResponse create(TaskRequest request);

    TaskResponse findById(Long id);

    List<TaskResponse> findAll();

    List<TaskResponse> findByStatus(TaskStatus status);

    List<TaskResponse> findByPriority(TaskPriority priority);

    List<TaskResponse> findByAssignedTo(String assignedTo);

    List<TaskResponse> search(String keyword);

    TaskResponse update(Long id, TaskRequest request);

    TaskResponse updateStatus(Long id, TaskStatus newStatus);

    void delete(Long id);

    Map<String, Long> getStatsByStatus();
}
