package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.VacationPayRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VacationPayRequestRepository extends JpaRepository<VacationPayRequest, Long> {

    @Query("""
            select request
            from VacationPayRequest request
            join fetch request.employee employee
            where employee.id = :employeeId
            order by request.submittedAt desc, request.id desc
            """)
    List<VacationPayRequest> findByEmployeeIdWithDetailsOrderBySubmittedAtDesc(@Param("employeeId") Long employeeId);

    @Query("""
            select request
            from VacationPayRequest request
            join fetch request.employee
            where request.startDate <= :endDate
              and request.endDate >= :startDate
            order by request.employee.name asc, request.startDate asc, request.id asc
            """)
    List<VacationPayRequest> findOverlappingWithDetails(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select request
            from VacationPayRequest request
            join fetch request.employee
            where request.id = :requestId
            """)
    Optional<VacationPayRequest> findByIdWithDetails(@Param("requestId") Long requestId);
}
