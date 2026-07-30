package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.BootAllowanceRequest;
import com.workforce.fabapp.enums.BootAllowanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BootAllowanceRequestRepository extends JpaRepository<BootAllowanceRequest, Long> {

    @Query("""
            select request
            from BootAllowanceRequest request
            join fetch request.employee employee
            join fetch request.receipt
            order by request.submittedAt desc, request.id desc
            """)
    List<BootAllowanceRequest> findAllWithDetailsOrderBySubmittedAtDesc();

    @Query("""
            select request
            from BootAllowanceRequest request
            join fetch request.employee employee
            join fetch request.receipt
            where employee.id = :employeeId
            order by request.submittedAt desc, request.id desc
            """)
    List<BootAllowanceRequest> findByEmployeeIdWithDetailsOrderBySubmittedAtDesc(@Param("employeeId") Long employeeId);

    @Query("""
            select request
            from BootAllowanceRequest request
            join fetch request.employee employee
            join fetch request.receipt
            where request.status = :status
            order by request.submittedAt desc, request.id desc
            """)
    List<BootAllowanceRequest> findByStatusWithDetailsOrderBySubmittedAtDesc(@Param("status") BootAllowanceStatus status);

    @Query("""
            select request
            from BootAllowanceRequest request
            join fetch request.employee employee
            join fetch request.receipt
            where request.id = :requestId
            """)
    Optional<BootAllowanceRequest> findByIdWithDetails(@Param("requestId") Long requestId);
}
