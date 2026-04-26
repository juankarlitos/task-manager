package com.taskmanager.repository;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByPriority(TaskPriority priority);

    List<Task> findByAssignedToIgnoreCase(String assignedTo);

    List<Task> findByStatusAndPriority(TaskStatus status, TaskPriority priority);

    @Query("SELECT t FROM Task t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
           " OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Task> searchByKeyword(String keyword);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.status = :status")
    long countByStatus(TaskStatus status);
}
