package com.workforce.fabapp.repository;

import com.workforce.fabapp.entity.TimesheetEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface TimesheetEntryRepository extends JpaRepository<TimesheetEntry, Long> {

    List<TimesheetEntry> findByTimesheetWeekId(Long timesheetWeekId);

    List<TimesheetEntry> findByTimesheetWeekIdAndWorkDate(Long timesheetWeekId, LocalDate workDate);

    @Query("""
            select e
            from TimesheetEntry e
            left join fetch e.job
            left join fetch e.workType
            left join fetch e.leaveType
            left join fetch e.jobRequest
            where e.timesheetWeek.id = :timesheetWeekId
            order by e.workDate asc, e.id asc
            """)
    List<TimesheetEntry> findByTimesheetWeekIdWithDetails(@Param("timesheetWeekId") Long timesheetWeekId);

    @Query("""
            select e
            from TimesheetEntry e
            join fetch e.timesheetWeek w
            join fetch w.employee emp
            left join fetch w.supervisor
            left join fetch e.job
            left join fetch e.workType
            left join fetch e.leaveType
            left join fetch e.jobRequest
            where w.id in :timesheetWeekIds
            order by w.weekStart desc, e.workDate asc, e.id asc
            """)
    List<TimesheetEntry> findByTimesheetWeekIdsWithDetails(@Param("timesheetWeekIds") Collection<Long> timesheetWeekIds);

    void deleteBySourceLeaveRequestId(Long sourceLeaveRequestId);

    List<TimesheetEntry> findByJobId(Long jobId);

    @Query("""
            select e
            from TimesheetEntry e
            join fetch e.timesheetWeek w
            join fetch w.employee
            left join fetch e.workType
            where e.job.id = :jobId
            order by w.weekStart desc, e.workDate asc, e.id asc
            """)
    List<TimesheetEntry> findByJobIdWithDetails(@Param("jobId") Long jobId);

    @Query("""
            select e.job.id, sum(e.hours)
            from TimesheetEntry e
            where e.leaveType is null
              and e.job.id in :jobIds
            group by e.job.id
            """)
    List<Object[]> sumHoursByJobIds(@Param("jobIds") Collection<Long> jobIds);

    @Query("""
            select e.job.id, e.workType.id, sum(e.hours)
            from TimesheetEntry e
            where e.leaveType is null
              and e.workType is not null
              and e.job.id in :jobIds
            group by e.job.id, e.workType.id
            """)
    List<Object[]> sumHoursByJobIdsAndWorkType(@Param("jobIds") Collection<Long> jobIds);

    List<TimesheetEntry> findByJobRequestId(Long jobRequestId);
}
