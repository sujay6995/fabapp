package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.DayOffWorkRequest;
import com.workforce.fabapp.enums.DayOffWorkRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DayOffWorkRequestRepository extends JpaRepository<DayOffWorkRequest, Long> {

    @Query("""
            select request from DayOffWorkRequest request
            join fetch request.employee employee
            join fetch request.supervisor
            where employee.id = :employeeId
            order by request.submittedAt desc, request.id desc
            """)
    List<DayOffWorkRequest> findByEmployeeIdWithDetails(@Param("employeeId") Long employeeId);

    @Query("""
            select request from DayOffWorkRequest request
            join fetch request.employee
            join fetch request.supervisor supervisor
            where supervisor.id = :supervisorId
            order by request.submittedAt desc, request.id desc
            """)
    List<DayOffWorkRequest> findBySupervisorIdWithDetails(@Param("supervisorId") Long supervisorId);

    @Query("""
            select request from DayOffWorkRequest request
            join fetch request.employee
            join fetch request.supervisor
            where request.id = :requestId
            """)
    Optional<DayOffWorkRequest> findByIdWithDetails(@Param("requestId") Long requestId);

    boolean existsByEmployeeIdAndWorkDateAndStatusNot(
            Long employeeId,
            LocalDate workDate,
            DayOffWorkRequestStatus status
    );
}
