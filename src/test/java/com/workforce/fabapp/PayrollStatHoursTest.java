package com.workforce.fabapp;

import com.workforce.fabapp.entity.*;
import com.workforce.fabapp.repository.*;
import com.workforce.fabapp.service.PayrollExportService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PayrollStatHoursTest {
    @Test void statPayDoesNotCreateOvertime() { check(44, null, 52, 0); }
    @Test void genuineOvertimeRemains() { check(48, null, 52, 4); }
    @Test void stalePlacementCannotCreateOvertime() { check(44, 8, 52, 0); }
    @Test void balancedPlacementIsApplied() { check(48, 4, 52, 4); }
    @Test void incompletePlacementCannotReduceOvertime() { check(48, 2, 52, 4); }

    private void check(int workedHours, Integer allocatedHours, int regular, int overtime) {
        var weeks = mock(TimesheetWeekRepository.class);
        var entries = mock(TimesheetEntryRepository.class);
        var dt = mock(DoubleTimeAllocationRepository.class);
        var ot = mock(OvertimeAllocationRepository.class);
        var vacation = mock(VacationPayRequestRepository.class);
        var service = new PayrollExportService(weeks, entries, dt, ot, vacation);
        var date = LocalDate.of(2026, 9, 6);
        var employee = Employee.builder().id(1L).name("Eric Foy").build();
        var week = TimesheetWeek.builder().id(1L).employee(employee).weekStart(date).build();
        var job = Job.builder().id(1L).code("226241").name("Worked job").build();
        var statJob = Job.builder().id(2L).code("226511").name("Stat Holiday").build();
        var category = WorkType.builder().id(1L).countsTowardOt(true).build();
        var stat = TimesheetEntry.builder().id(1L).timesheetWeek(week).workDate(date.plusDays(1))
                .job(statJob).workType(category).hours(BigDecimal.valueOf(8)).build();
        var worked = TimesheetEntry.builder().id(2L).timesheetWeek(week).workDate(date.plusDays(2))
                .job(job).workType(category).hours(BigDecimal.valueOf(workedHours)).build();
        when(weeks.findByWeekStartWithPeople(date)).thenReturn(List.of(week));
        when(entries.findByTimesheetWeekIdsWithDetails(List.of(1L))).thenReturn(List.of(stat, worked));
        if (allocatedHours != null) {
            var allocation = new OvertimeAllocation();
            allocation.setTimesheetWeek(week);
            allocation.setSourceEntry(worked);
            allocation.setJob(job);
            allocation.setHours(BigDecimal.valueOf(allocatedHours));
            when(ot.findByTimesheetWeekIdsWithDetailsOrderBySortOrderAscIdAsc(List.of(1L)))
                    .thenReturn(List.of(allocation));
        }
        var result = service.getPayrollReview(date);
        assertEquals(0, result.getRegularHours().compareTo(BigDecimal.valueOf(regular)));
        assertEquals(0, result.getOtHours().compareTo(BigDecimal.valueOf(overtime)));
        assertEquals(0, result.getTotalHours().compareTo(BigDecimal.valueOf(workedHours + 8)));
    }
}
