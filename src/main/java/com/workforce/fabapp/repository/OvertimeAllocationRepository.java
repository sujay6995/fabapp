package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.OvertimeAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OvertimeAllocationRepository extends JpaRepository<OvertimeAllocation, Long> {

    List<OvertimeAllocation> findByTimesheetWeekIdOrderBySortOrderAscIdAsc(Long timesheetWeekId);

    @Query("""
            select allocation
            from OvertimeAllocation allocation
            join fetch allocation.timesheetWeek
            left join fetch allocation.job
            where allocation.timesheetWeek.id = :timesheetWeekId
            order by allocation.sortOrder asc, allocation.id asc
            """)
    List<OvertimeAllocation> findByTimesheetWeekIdWithDetailsOrderBySortOrderAscIdAsc(
            @Param("timesheetWeekId") Long timesheetWeekId
    );

    void deleteByTimesheetWeekId(Long timesheetWeekId);
}
