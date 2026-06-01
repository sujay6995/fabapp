package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.WorkerTask;
import com.workforce.fabapp.enums.WorkerTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkerTaskRepository extends JpaRepository<WorkerTask, Long> {

    List<WorkerTask> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<WorkerTask> findBySupervisorIdOrderByCreatedAtDesc(Long supervisorId);

    List<WorkerTask> findByEmployeeSupervisorIdOrderByCreatedAtDesc(Long supervisorId);

    List<WorkerTask> findByStatusOrderByCreatedAtDesc(WorkerTaskStatus status);

    @Query("""
            select task
            from WorkerTask task
            join fetch task.employee
            left join fetch task.supervisor
            left join fetch task.job
            where task.employee.id = :employeeId
            order by task.createdAt desc
            """)
    List<WorkerTask> findByEmployeeIdWithDetailsOrderByCreatedAtDesc(@Param("employeeId") Long employeeId);

    @Query("""
            select task
            from WorkerTask task
            join fetch task.employee employee
            left join fetch task.supervisor
            left join fetch task.job
            where employee.supervisor.id = :supervisorId
            order by task.createdAt desc
            """)
    List<WorkerTask> findByEmployeeSupervisorIdWithDetailsOrderByCreatedAtDesc(@Param("supervisorId") Long supervisorId);

    @Query("""
            select task
            from WorkerTask task
            join fetch task.employee
            left join fetch task.supervisor
            left join fetch task.job
            order by task.createdAt desc
            """)
    List<WorkerTask> findAllWithDetailsOrderByCreatedAtDesc();

    @Query("""
            select task
            from WorkerTask task
            join fetch task.employee
            left join fetch task.supervisor
            left join fetch task.job
            where task.id = :taskId
            """)
    Optional<WorkerTask> findByIdWithDetails(@Param("taskId") Long taskId);
}
