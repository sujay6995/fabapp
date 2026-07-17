package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.OvertimeAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OvertimeAllocationRepository extends JpaRepository<OvertimeAllocation, Long> {

    List<OvertimeAllocation> findByTimesheetWeekIdOrderBySortOrderAscIdAsc(Long timesheetWeekId);

    @Query("""
            select allocation
            from OvertimeAllocation allocation
            join fetch allocation.timesheetWeek
            left join fetch allocation.sourceEntry sourceEntry
            left join fetch sourceEntry.job sourceJob
            left join fetch sourceEntry.jobRequest sourceJobRequest
            left join fetch sourceJobRequest.openedJob
            left join fetch allocation.job
            where allocation.timesheetWeek.id = :timesheetWeekId
            order by allocation.sortOrder asc, allocation.id asc
            """)
    List<OvertimeAllocation> findByTimesheetWeekIdWithDetailsOrderBySortOrderAscIdAsc(
            @Param("timesheetWeekId") Long timesheetWeekId
    );

    @Query("""
            select allocation
            from OvertimeAllocation allocation
            join fetch allocation.timesheetWeek week
            left join fetch allocation.sourceEntry sourceEntry
            left join fetch sourceEntry.job sourceJob
            left join fetch sourceEntry.jobRequest sourceJobRequest
            left join fetch sourceJobRequest.openedJob
            left join fetch allocation.job
            where week.id in :timesheetWeekIds
            order by week.id asc, allocation.sortOrder asc, allocation.id asc
            """)
    List<OvertimeAllocation> findByTimesheetWeekIdsWithDetailsOrderBySortOrderAscIdAsc(
            @Param("timesheetWeekIds") Collection<Long> timesheetWeekIds
    );

    void deleteByTimesheetWeekId(Long timesheetWeekId);
}
