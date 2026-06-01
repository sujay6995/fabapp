package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.LeaveRequest;
import com.workforce.fabapp.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeIdAndStatusOrderByStartDateDesc(Long employeeId, LeaveStatus status);
    long countByStatus(LeaveStatus status);

    List<LeaveRequest> findByEmployeeIdOrderByStartDateDesc(Long employeeId);

    @Query("""
            select leave
            from LeaveRequest leave
            join fetch leave.leaveType
            where leave.employee.id = :employeeId
              and leave.status = :status
              and leave.startDate <= :end
              and leave.endDate >= :start
            order by leave.startDate desc
            """)
    List<LeaveRequest> findByEmployeeIdAndStatusOverlapping(
            @Param("employeeId") Long employeeId,
            @Param("status") LeaveStatus status,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
            select leave
            from LeaveRequest leave
            join fetch leave.employee employee
            join fetch leave.leaveType
            where employee.id in :employeeIds
              and leave.status = :status
              and leave.startDate <= :end
              and leave.endDate >= :start
            order by employee.id asc, leave.startDate desc
            """)
    List<LeaveRequest> findByEmployeeIdsAndStatusOverlapping(
            @Param("employeeIds") Collection<Long> employeeIds,
            @Param("status") LeaveStatus status,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    List<LeaveRequest> findByApproverIdAndStatus(Long approverId, LeaveStatus status);
}
