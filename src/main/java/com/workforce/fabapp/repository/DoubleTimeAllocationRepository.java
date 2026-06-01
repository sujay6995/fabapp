package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.DoubleTimeAllocation;
import com.workforce.fabapp.enums.DoubleTimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoubleTimeAllocationRepository extends JpaRepository<DoubleTimeAllocation, Long> {

    List<DoubleTimeAllocation> findByTimesheetWeekIdAndStatus(Long timesheetWeekId, DoubleTimeStatus status);

    @Query("""
            select allocation
            from DoubleTimeAllocation allocation
            join fetch allocation.timesheetWeek week
            join fetch week.employee
            left join fetch allocation.timesheetEntry
            left join fetch allocation.job
            where week.id = :timesheetWeekId
              and allocation.status = :status
            """)
    List<DoubleTimeAllocation> findByTimesheetWeekIdAndStatusWithDetails(
            @Param("timesheetWeekId") Long timesheetWeekId,
            @Param("status") DoubleTimeStatus status
    );

    @Query("""
            select allocation
            from DoubleTimeAllocation allocation
            join fetch allocation.timesheetWeek week
            join fetch week.employee
            left join fetch allocation.timesheetEntry
            left join fetch allocation.job
            where allocation.id = :allocationId
            """)
    Optional<DoubleTimeAllocation> findByIdWithDetails(@Param("allocationId") Long allocationId);

    List<DoubleTimeAllocation> findByTimesheetWeekId(Long timesheetWeekId);

    List<DoubleTimeAllocation> findByJobIdAndStatus(Long jobId, DoubleTimeStatus status);
}
