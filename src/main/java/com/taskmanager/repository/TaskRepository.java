package com.taskmanager.repository;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // ── Consultas filtradas por owner ─────────────────────────────────────────

    List<Task> findByOwner(User owner);

    Optional<Task> findByIdAndOwner(Long id, User owner);

    List<Task> findByStatusAndOwner(TaskStatus status, User owner);

    List<Task> findByPriorityAndOwner(TaskPriority priority, User owner);

    List<Task> findByAssignedToIgnoreCaseAndOwner(String assignedTo, User owner);

    long countByStatusAndOwner(TaskStatus status, User owner);

    @Query("SELECT t FROM Task t WHERE t.owner = :owner AND (" +
           "LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Task> searchByKeywordAndOwner(String keyword, User owner);

    // ── Métodos originales (sin filtro, conservados para compatibilidad) ───────

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByPriority(TaskPriority priority);

    List<Task> findByAssignedToIgnoreCase(String assignedTo);

    @Query("SELECT t FROM Task t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
           " OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Task> searchByKeyword(String keyword);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.status = :status")
    long countByStatus(TaskStatus status);
}
