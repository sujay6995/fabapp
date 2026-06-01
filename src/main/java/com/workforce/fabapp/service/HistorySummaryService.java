package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.*;
import com.workforce.fabapp.entity.Job;
import com.workforce.fabapp.entity.TimesheetEntry;
import com.workforce.fabapp.entity.TimesheetWeek;
import com.workforce.fabapp.enums.TimesheetStatus;
import com.workforce.fabapp.repository.EmployeeRepository;
import com.workforce.fabapp.repository.JobRepository;
import com.workforce.fabapp.repository.TimesheetEntryRepository;
import com.workforce.fabapp.repository.TimesheetWeekRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistorySummaryService {

    private static final BigDecimal REGULAR_HOURS_PER_WEEK = BigDecimal.valueOf(44);

    private final TimesheetWeekRepository timesheetWeekRepository;
    private final TimesheetEntryRepository timesheetEntryRepository;
    private final EmployeeRepository employeeRepository;
    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    public HistorySummaryDto getSummary(LocalDate start, LocalDate end) {
        LocalDate safeStart = start != null ? normalizeToSunday(start) : LocalDate.now().minusWeeks(8);
        LocalDate safeEnd = end != null ? end : LocalDate.now();

        List<TimesheetWeek> weeks = timesheetWeekRepository
                .findByWeekStartBetweenWithPeopleOrderByWeekStartDesc(safeStart, safeEnd);
        Map<Long, List<TimesheetEntry>> entriesByWeekId = loadEntriesByWeekId(weeks);

        List<HistoryWeekDto> weekDtos = weeks.stream()
                .map(week -> mapWeek(week, entriesByWeekId.getOrDefault(week.getId(), Collections.emptyList())))
                .toList();

        Set<Long> employeeIds = new HashSet<>();
        weekDtos.forEach(w -> employeeIds.add(w.getEmployeeId()));

        return HistorySummaryDto.builder()
                .start(safeStart)
                .end(safeEnd)
                .weekCount(weekDtos.size())
                .employeeCount(employeeIds.size())
                .totalHours(sum(weekDtos.stream().map(HistoryWeekDto::getTotalHours).toList()))
                .regularHours(sum(weekDtos.stream().map(HistoryWeekDto::getRegularHours).toList()))
                .otHours(sum(weekDtos.stream().map(HistoryWeekDto::getOtHours).toList()))
                .leaveHours(sum(weekDtos.stream().map(HistoryWeekDto::getLeaveHours).toList()))
                .submittedCount(weekDtos.stream().filter(w -> "SUBMITTED".equals(w.getStatus())).count())
                .approvedCount(weekDtos.stream().filter(w -> "APPROVED".equals(w.getStatus())).count())
                .payrollLockedCount(weekDtos.stream().filter(w -> Boolean.TRUE.equals(w.getPayrollLocked())).count())
                .weeks(weekDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public EmployeeHistoryDto getEmployeeHistory(Long employeeId, LocalDate start, LocalDate end) {
        var employee = employeeRepository.findByIdWithProfile(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        LocalDate safeStart = start != null ? normalizeToSunday(start) : LocalDate.now().minusWeeks(8);
        LocalDate safeEnd = end != null ? end : LocalDate.now();

        List<TimesheetWeek> weeks = timesheetWeekRepository
                .findByEmployeeIdAndWeekStartBetweenWithPeopleOrderByWeekStartDesc(employeeId, safeStart, safeEnd);
        Map<Long, List<TimesheetEntry>> entriesByWeekId = loadEntriesByWeekId(weeks);
        List<HistoryWeekDto> weekDtos = weeks.stream()
                .map(week -> mapWeek(week, entriesByWeekId.getOrDefault(week.getId(), Collections.emptyList())))
                .toList();

        return EmployeeHistoryDto.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getName())
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .crewName(employee.getCrew() != null ? employee.getCrew().getName() : null)
                .supervisorName(employee.getSupervisor() != null ? employee.getSupervisor().getName() : null)
                .totalHours(sum(weekDtos.stream().map(HistoryWeekDto::getTotalHours).toList()))
                .regularHours(sum(weekDtos.stream().map(HistoryWeekDto::getRegularHours).toList()))
                .otHours(sum(weekDtos.stream().map(HistoryWeekDto::getOtHours).toList()))
                .leaveHours(sum(weekDtos.stream().map(HistoryWeekDto::getLeaveHours).toList()))
                .submittedWeeks(weekDtos.stream().filter(w -> "SUBMITTED".equals(w.getStatus())).count())
                .approvedWeeks(weekDtos.stream().filter(w -> "APPROVED".equals(w.getStatus())).count())
                .payrollLockedWeeks(weekDtos.stream().filter(w -> Boolean.TRUE.equals(w.getPayrollLocked())).count())
                .weeks(weekDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public JobHistoryDto getJobHistory(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));

        List<JobHistoryLineDto> lines = timesheetEntryRepository.findByJobIdWithDetails(jobId)
                .stream()
                .filter(e -> e.getLeaveType() == null)
                .map(this::mapJobLine)
                .toList();

        BigDecimal actualHours = sum(lines.stream().map(JobHistoryLineDto::getHours).toList());

        BigDecimal budget = job.getTotalBudgetHours() != null ? job.getTotalBudgetHours() : BigDecimal.ZERO;
        BigDecimal remaining = budget.compareTo(BigDecimal.ZERO) > 0
                ? budget.subtract(actualHours)
                : BigDecimal.ZERO;

        BigDecimal usedPercent = BigDecimal.ZERO;
        if (budget.compareTo(BigDecimal.ZERO) > 0) {
            usedPercent = actualHours
                    .multiply(BigDecimal.valueOf(100))
                    .divide(budget, 2, RoundingMode.HALF_UP);
        }

        return JobHistoryDto.builder()
                .jobId(job.getId())
                .jobNumber(job.getCode())
                .jobName(job.getName())
                .xNumber(job.getXNumber())
                .category(job.getCategory())
                .active(job.getActive())
                .closed(job.getClosed())
                .totalBudgetHours(scale(budget))
                .warningPercent(job.getWarningPercent())
                .budgetLocked(job.getBudgetLocked())
                .actualHours(scale(actualHours))
                .regularHours(scale(actualHours))
                .otHours(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .doubleTimeHours(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .budgetRemainingHours(scale(remaining))
                .budgetUsedPercent(scale(usedPercent))
                .lines(lines)
                .build();
    }

    private HistoryWeekDto mapWeek(TimesheetWeek week, List<TimesheetEntry> entries) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal eligible = BigDecimal.ZERO;
        BigDecimal leave = BigDecimal.ZERO;

        for (TimesheetEntry entry : entries) {
            BigDecimal hours = entry.getHours() != null ? entry.getHours() : BigDecimal.ZERO;
            total = total.add(hours);

            if (entry.getLeaveType() != null) {
                leave = leave.add(hours);
            } else if (entry.getWorkType() != null && Boolean.TRUE.equals(entry.getWorkType().getCountsTowardOt())) {
                eligible = eligible.add(hours);
            }
        }

        BigDecimal regular = eligible.min(REGULAR_HOURS_PER_WEEK);
        BigDecimal ot = eligible.subtract(REGULAR_HOURS_PER_WEEK).max(BigDecimal.ZERO);

        return HistoryWeekDto.builder()
                .weekId(week.getId())
                .employeeId(week.getEmployee().getId())
                .employeeName(week.getEmployee().getName())
                .weekStart(week.getWeekStart())
                .weekEnd(week.getWeekStart().plusDays(6))
                .status(week.getStatus() != null ? week.getStatus().name() : null)
                .payrollLocked(week.getPayrollLocked())
                .totalHours(scale(total))
                .regularHours(scale(regular))
                .otHours(scale(ot))
                .leaveHours(scale(leave))
                .submittedAt(week.getSubmittedAt())
                .approvedAt(week.getApprovedAt())
                .supervisorId(week.getSupervisor() != null ? week.getSupervisor().getId() : null)
                .supervisorName(week.getSupervisor() != null ? week.getSupervisor().getName() : null)
                .build();
    }

    private Map<Long, List<TimesheetEntry>> loadEntriesByWeekId(List<TimesheetWeek> weeks) {
        List<Long> weekIds = weeks.stream().map(TimesheetWeek::getId).toList();
        if (weekIds.isEmpty()) {
            return Map.of();
        }
        return timesheetEntryRepository.findByTimesheetWeekIdsWithDetails(weekIds)
                .stream()
                .collect(Collectors.groupingBy(entry -> entry.getTimesheetWeek().getId()));
    }

    private JobHistoryLineDto mapJobLine(TimesheetEntry entry) {
        TimesheetWeek week = entry.getTimesheetWeek();

        return JobHistoryLineDto.builder()
                .entryId(entry.getId())
                .employeeId(week.getEmployee().getId())
                .employeeName(week.getEmployee().getName())
                .workDate(entry.getWorkDate())
                .weekStart(week.getWeekStart())
                .workTypeName(entry.getWorkType() != null ? entry.getWorkType().getName() : null)
                .hours(scale(entry.getHours() != null ? entry.getHours() : BigDecimal.ZERO))
                .notes(entry.getNotes())
                .weekStatus(week.getStatus() != null ? week.getStatus().name() : null)
                .build();
    }

    private LocalDate normalizeToSunday(LocalDate date) {
        while (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
            date = date.minusDays(1);
        }
        return date;
    }

    private BigDecimal sum(List<BigDecimal> values) {
        return scale(values.stream()
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
