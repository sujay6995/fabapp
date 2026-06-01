package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.*;
import com.workforce.fabapp.entity.*;
import com.workforce.fabapp.enums.AttendanceEventKind;
import com.workforce.fabapp.enums.LeaveStatus;
import com.workforce.fabapp.enums.TimesheetStatus;
import com.workforce.fabapp.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimesheetService {

    private static final BigDecimal REGULAR_HOURS_PER_WEEK = BigDecimal.valueOf(44);
    private static final BigDecimal OT_MULTIPLIER = BigDecimal.valueOf(1.5);

    private final TimesheetWeekRepository timesheetWeekRepository;
    private final TimesheetEntryRepository timesheetEntryRepository;
    private final EmployeeRepository employeeRepository;
    private final JobRepository jobRepository;
    private final WorkTypeRepository workTypeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final CrewScheduleRepository crewScheduleRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AuditLogRepository auditLogRepository;
    private final JobRequestRepository jobRequestRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final OvertimeAllocationService overtimeAllocationService;

    private record WeekIssueContext(
            Map<Long, Map<LocalDate, CrewSchedule>> schedulesByCrewId,
            Map<Long, List<LeaveRequest>> approvedLeavesByEmployeeId,
            Map<Long, Map<LocalDate, AttendanceEvent>> attendanceByEmployeeIdAndDate
    ) {
        private static WeekIssueContext empty() {
            return new WeekIssueContext(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        }
    }

    @Transactional
    @Cacheable(value = "timesheetWeeks", key = "'employee:' + #employeeId + ':' + #weekStart")
    public TimesheetWeekResponseDto getOrCreateWeek(Long employeeId, LocalDate weekStart) {
        LocalDate normalizedWeekStart = normalizeToSunday(weekStart);

        TimesheetWeek week = timesheetWeekRepository
                .findByEmployeeIdAndWeekStartWithPeople(employeeId, normalizedWeekStart)
                .orElseGet(() -> createWeek(employeeId, normalizedWeekStart));

        return mapWeek(week);
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public TimesheetEntryResponseDto addEntry(TimesheetEntryRequestDto dto, String actor) {
        TimesheetWeek week = timesheetWeekRepository.findById(dto.getTimesheetWeekId())
                .orElseThrow(() -> new EntityNotFoundException("Timesheet week not found"));

        touchWeekForEdit(week, actor);

        TimesheetEntry entry = buildEntryFromDto(new TimesheetEntry(), week, dto);
        TimesheetEntry saved = timesheetEntryRepository.save(entry);

        return mapEntry(saved);
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public TimesheetEntryResponseDto updateEntry(Long entryId, TimesheetEntryRequestDto dto, String actor) {
        TimesheetEntry entry = timesheetEntryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet entry not found"));

        TimesheetWeek week = entry.getTimesheetWeek();
        touchWeekForEdit(week, actor);

        TimesheetEntry saved = timesheetEntryRepository.save(buildEntryFromDto(entry, week, dto));
        return mapEntry(saved);
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public void deleteEntry(Long entryId, String actor) {
        TimesheetEntry entry = timesheetEntryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet entry not found"));

        touchWeekForEdit(entry.getTimesheetWeek(), actor);
        timesheetEntryRepository.delete(entry);
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public TimesheetWeekResponseDto submitWeek(Long weekId) {
        TimesheetWeek week = timesheetWeekRepository.findByIdWithPeople(weekId)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet week not found"));

        if (Boolean.TRUE.equals(week.getPayrollLocked()) || week.getStatus() == TimesheetStatus.PAYROLL_LOCKED) {
            throw new IllegalStateException("Week is payroll locked.");
        }

        List<TimesheetIssueDto> issues = getWeekIssuesInternal(week);
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Week has validation issues and cannot be submitted.");
        }

        overtimeAllocationService.ensureDefaultAllocations(weekId, week.getEmployee().getName());
        week.setStatus(TimesheetStatus.SUBMITTED);
        week.setSubmittedAt(LocalDateTime.now());
        timesheetWeekRepository.save(week);

        auditLogRepository.save(AuditLog.builder()
                .actor(week.getEmployee().getName())
                .item("Week submitted for approval")
                .at(LocalDateTime.now())
                .build());

        return mapWeek(week);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "timesheetIssues", key = "#weekId")
    public List<TimesheetIssueDto> getWeekIssues(Long weekId) {
        TimesheetWeek week = timesheetWeekRepository.findByIdWithPeople(weekId)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet week not found"));

        return getWeekIssuesInternal(week);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "timesheetWeeks", key = "'supervisor:' + #supervisorId + ':' + #weekStart")
    public List<TimesheetWeekResponseDto> getSupervisorWeeks(Long supervisorId, LocalDate weekStart) {
        LocalDate normalizedWeekStart = normalizeToSunday(weekStart);

        List<TimesheetWeek> weeks = timesheetWeekRepository.findBySupervisorIdAndWeekStartWithPeople(supervisorId, normalizedWeekStart)
                .stream()
                .sorted(Comparator.comparing(w -> w.getEmployee().getName()))
                .toList();

        return mapWeeks(weeks);
    }

    private TimesheetWeek createWeek(Long employeeId, LocalDate weekStart) {
        Employee employee = employeeRepository.findByIdWithProfile(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + employeeId));

        TimesheetWeek week = TimesheetWeek.builder()
                .employee(employee)
                .weekStart(weekStart)
                .status(TimesheetStatus.DRAFT)
                .submittedAt(null)
                .approvedAt(null)
                .payrollLocked(Boolean.FALSE)
                .supervisor(employee.getSupervisor())
                .build();

        return timesheetWeekRepository.save(week);
    }

    private LocalDate normalizeToSunday(LocalDate date) {
        while (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
            date = date.minusDays(1);
        }
        return date;
    }

    private void touchWeekForEdit(TimesheetWeek week, String actor) {
        if (Boolean.TRUE.equals(week.getPayrollLocked()) || week.getStatus() == TimesheetStatus.PAYROLL_LOCKED) {
            throw new IllegalStateException("Week is payroll locked.");
        }

        if (week.getStatus() == TimesheetStatus.APPROVED) {
            week.setStatus(TimesheetStatus.REOPENED);
            week.setApprovedAt(null);

            auditLogRepository.save(AuditLog.builder()
                    .actor(actor)
                    .item(week.getEmployee().getName() + " week reopened after edit")
                    .at(LocalDateTime.now())
                    .build());
        } else if (week.getStatus() == TimesheetStatus.SUBMITTED) {
            week.setStatus(TimesheetStatus.DRAFT);
        }

        timesheetWeekRepository.save(week);
    }

    private TimesheetEntry buildEntryFromDto(TimesheetEntry entry, TimesheetWeek week, TimesheetEntryRequestDto dto) {
        entry.setTimesheetWeek(week);
        entry.setWorkDate(dto.getWorkDate());
        entry.setHours(dto.getHours());
        entry.setNotes(dto.getNotes());

        Job job = dto.getJobId() != null
                ? jobRepository.findById(dto.getJobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found"))
                : null;

        JobRequest jobRequest = dto.getJobRequestId() != null
                ? jobRequestRepository.findById(dto.getJobRequestId())
                .orElseThrow(() -> new EntityNotFoundException("Job request not found"))
                : null;

        if (job != null && jobRequest != null) {
            throw new IllegalStateException("Entry cannot have both opened job and pending job request.");
        }

        entry.setJob(job);
        entry.setJobRequest(jobRequest);

        entry.setWorkType(dto.getWorkTypeId() != null
                ? workTypeRepository.findById(dto.getWorkTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Work type not found"))
                : null);

        if (entry.getWorkType() != null
                && "Overburn".equalsIgnoreCase(entry.getWorkType().getName())) {
            entry.setNotes("Management");
        }

        entry.setLeaveType(dto.getLeaveTypeId() != null
                ? leaveTypeRepository.findById(dto.getLeaveTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Leave type not found"))
                : null);

        if (entry.getAutoLeave() == null) {
            entry.setAutoLeave(Boolean.FALSE);
        }

        return entry;
    }

    private List<TimesheetIssueDto> getWeekIssuesInternal(TimesheetWeek week) {
        List<TimesheetEntry> entries = timesheetEntryRepository.findByTimesheetWeekIdWithDetails(week.getId());
        return getWeekIssuesInternal(week, entries);
    }

    private List<TimesheetIssueDto> getWeekIssuesInternal(TimesheetWeek week, List<TimesheetEntry> preloadedEntries) {
        return getWeekIssuesInternal(week, preloadedEntries, loadIssueContext(List.of(week)));
    }

    private List<TimesheetIssueDto> getWeekIssuesInternal(
            TimesheetWeek week,
            List<TimesheetEntry> preloadedEntries,
            WeekIssueContext context
    ) {
        List<TimesheetIssueDto> issues = new ArrayList<>();
        Long employeeId = week.getEmployee().getId();
        Long crewId = week.getEmployee().getCrew().getId();
        Map<LocalDate, CrewSchedule> schedulesByDate = context.schedulesByCrewId()
                .getOrDefault(crewId, Collections.emptyMap());
        List<LeaveRequest> approvedLeaves = context.approvedLeavesByEmployeeId()
                .getOrDefault(employeeId, Collections.emptyList());

        Map<LocalDate, List<TimesheetEntry>> entriesByDate = preloadedEntries.stream()
                .collect(Collectors.groupingBy(TimesheetEntry::getWorkDate));

        Map<LocalDate, AttendanceEvent> attendanceByDate = context.attendanceByEmployeeIdAndDate()
                .getOrDefault(employeeId, Collections.emptyMap());

        for (int i = 0; i < 7; i++) {
            LocalDate date = week.getWeekStart().plusDays(i);

            CrewSchedule schedule = schedulesByDate.get(date);
            boolean isWorkday = schedule != null && Boolean.TRUE.equals(schedule.getIsWorkday());

            boolean hasApprovedLeave = approvedLeaves
                    .stream()
                    .anyMatch(l -> !date.isBefore(l.getStartDate()) && !date.isAfter(l.getEndDate()));

            List<TimesheetEntry> entries = entriesByDate.getOrDefault(date, Collections.emptyList());

            BigDecimal totalForDay = entries.stream()
                    .map(e -> e.getHours() != null ? e.getHours() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            AttendanceEvent attendance = attendanceByDate.get(date);
            boolean attendanceExcused = attendance != null
                    && (attendance.getKind() == AttendanceEventKind.SICK_DAY
                    || attendance.getKind() == AttendanceEventKind.MISSED_DAY
                    || attendance.getKind() == AttendanceEventKind.VACATION);

            if (isWorkday && !hasApprovedLeave && !attendanceExcused
                    && totalForDay.compareTo(BigDecimal.ZERO) == 0) {
                issues.add(TimesheetIssueDto.builder()
                        .type("Missed Day")
                        .date(date)
                        .message(date + " is a scheduled crew workday with no time entered.")
                        .build());
            }

            for (TimesheetEntry entry : entries) {
                BigDecimal hours = entry.getHours() != null ? entry.getHours() : BigDecimal.ZERO;
                boolean isLeaveEntry = entry.getLeaveType() != null;

                if (isLeaveEntry) {
                    if (hours.compareTo(BigDecimal.ZERO) <= 0) {
                        issues.add(TimesheetIssueDto.builder()
                                .type("Missing Leave Hours")
                                .date(date)
                                .message(date + " has a leave entry with no hours.")
                                .build());
                    }
                    continue;
                }

                if (hours.compareTo(BigDecimal.ZERO) > 0 && entry.getWorkType() == null) {
                    issues.add(TimesheetIssueDto.builder()
                            .type("Missing Work Type")
                            .date(date)
                            .message(date + " has hours without a work category.")
                            .build());
                }

                if (hours.compareTo(BigDecimal.ZERO) > 0
                        && entry.getJob() == null
                        && entry.getJobRequest() == null) {
                    issues.add(TimesheetIssueDto.builder()
                            .type("Missing Job")
                            .date(date)
                            .message(date + " has worked hours without a job number.")
                            .build());
                }

                if (hours.compareTo(BigDecimal.ZERO) > 0
                        && entry.getWorkType() != null
                        && "Other".equalsIgnoreCase(entry.getWorkType().getName())
                        && (entry.getNotes() == null || entry.getNotes().isBlank())) {
                    issues.add(TimesheetIssueDto.builder()
                            .type("Other Description")
                            .date(date)
                            .message(date + " uses category Other and needs a description note.")
                            .build());
                }
            }
        }

        return issues;
    }

    private List<TimesheetWeekResponseDto> mapWeeks(List<TimesheetWeek> weeks) {
        if (weeks.isEmpty()) {
            return List.of();
        }

        Map<Long, List<TimesheetEntry>> entriesByWeekId = loadEntriesByWeekId(weeks);
        WeekIssueContext context = loadIssueContext(weeks);

        return weeks.stream()
                .map(week -> mapWeek(
                        week,
                        entriesByWeekId.getOrDefault(week.getId(), Collections.emptyList()),
                        context
                ))
                .toList();
    }

    private TimesheetWeekResponseDto mapWeek(TimesheetWeek week) {
        List<TimesheetEntry> entries = timesheetEntryRepository.findByTimesheetWeekIdWithDetails(week.getId());
        return mapWeek(week, entries, loadIssueContext(List.of(week)));
    }

    private TimesheetWeekResponseDto mapWeek(
            TimesheetWeek week,
            List<TimesheetEntry> entries,
            WeekIssueContext context
    ) {
        BigDecimal totalHours = BigDecimal.ZERO;
        BigDecimal eligibleHours = BigDecimal.ZERO;
        BigDecimal leaveHours = BigDecimal.ZERO;

        for (TimesheetEntry entry : entries) {
            BigDecimal hours = entry.getHours() != null ? entry.getHours() : BigDecimal.ZERO;
            totalHours = totalHours.add(hours);

            if (entry.getLeaveType() != null) {
                leaveHours = leaveHours.add(hours);
            } else if (entry.getWorkType() != null && Boolean.TRUE.equals(entry.getWorkType().getCountsTowardOt())) {
                eligibleHours = eligibleHours.add(hours);
            }
        }

        BigDecimal regularHours = eligibleHours.min(REGULAR_HOURS_PER_WEEK);
        BigDecimal otHours = eligibleHours.subtract(REGULAR_HOURS_PER_WEEK).max(BigDecimal.ZERO);

        return TimesheetWeekResponseDto.builder()
                .id(week.getId())
                .employeeId(week.getEmployee().getId())
                .employeeName(week.getEmployee().getName())
                .weekStart(week.getWeekStart())
                .status(week.getStatus())
                .submittedAt(week.getSubmittedAt())
                .approvedAt(week.getApprovedAt())
                .payrollLocked(week.getPayrollLocked())
                .supervisorId(week.getSupervisor() != null ? week.getSupervisor().getId() : null)
                .supervisorName(week.getSupervisor() != null ? week.getSupervisor().getName() : null)
                .entries(entries.stream().map(this::mapEntry).toList())
                .issues(getWeekIssuesInternal(week, entries, context))
                .totals(TimesheetTotalsDto.builder()
                        .totalHours(scale(totalHours))
                        .eligibleHours(scale(eligibleHours))
                        .leaveHours(scale(leaveHours))
                        .regularHours(scale(regularHours))
                        .otHours(scale(otHours))
                        .otPayFactor(scale(OT_MULTIPLIER))
                        .build())
                .build();
    }

    private Map<Long, List<TimesheetEntry>> loadEntriesByWeekId(List<TimesheetWeek> weeks) {
        List<Long> weekIds = weeks.stream().map(TimesheetWeek::getId).toList();
        if (weekIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return timesheetEntryRepository.findByTimesheetWeekIdsWithDetails(weekIds)
                .stream()
                .collect(Collectors.groupingBy(entry -> entry.getTimesheetWeek().getId()));
    }

    private WeekIssueContext loadIssueContext(List<TimesheetWeek> weeks) {
        if (weeks.isEmpty()) {
            return WeekIssueContext.empty();
        }

        Set<Long> employeeIds = weeks.stream()
                .map(week -> week.getEmployee().getId())
                .collect(Collectors.toSet());
        Set<Long> crewIds = weeks.stream()
                .map(week -> week.getEmployee().getCrew().getId())
                .collect(Collectors.toSet());
        LocalDate start = weeks.stream()
                .map(TimesheetWeek::getWeekStart)
                .min(LocalDate::compareTo)
                .orElseThrow();
        LocalDate end = weeks.stream()
                .map(week -> week.getWeekStart().plusDays(6))
                .max(LocalDate::compareTo)
                .orElseThrow();

        Map<Long, Map<LocalDate, CrewSchedule>> schedulesByCrewId = crewScheduleRepository
                .findByCrewIdsAndWorkDateBetween(crewIds, start, end)
                .stream()
                .collect(Collectors.groupingBy(
                        schedule -> schedule.getCrew().getId(),
                        Collectors.toMap(CrewSchedule::getWorkDate, Function.identity(), (first, second) -> second)
                ));

        Map<Long, List<LeaveRequest>> approvedLeavesByEmployeeId = leaveRequestRepository
                .findByEmployeeIdsAndStatusOverlapping(employeeIds, LeaveStatus.APPROVED, start, end)
                .stream()
                .collect(Collectors.groupingBy(leave -> leave.getEmployee().getId()));

        Map<Long, Map<LocalDate, AttendanceEvent>> attendanceByEmployeeIdAndDate = new HashMap<>();
        attendanceEventRepository.findByEmployeeIdsAndEventDateBetween(employeeIds, start, end)
                .forEach(event -> attendanceByEmployeeIdAndDate
                        .computeIfAbsent(event.getEmployee().getId(), ignored -> new HashMap<>())
                        .put(event.getEventDate(), event));

        return new WeekIssueContext(schedulesByCrewId, approvedLeavesByEmployeeId, attendanceByEmployeeIdAndDate);
    }

    private TimesheetEntryResponseDto mapEntry(TimesheetEntry entry) {
        return TimesheetEntryResponseDto.builder()
                .id(entry.getId())
                .workDate(entry.getWorkDate())
                .jobId(entry.getJob() != null ? entry.getJob().getId() : null)
                .jobCode(entry.getJob() != null ? entry.getJob().getCode() : null)
                .jobName(entry.getJob() != null ? entry.getJob().getName() : null)
                .jobRequestId(entry.getJobRequest() != null ? entry.getJobRequest().getId() : null)
                .pendingJobNumber(entry.getJobRequest() != null ? entry.getJobRequest().getRequestedJobNumber() : null)
                .pendingJobStatus(entry.getJobRequest() != null ? entry.getJobRequest().getStatus().name() : null)
                .workTypeId(entry.getWorkType() != null ? entry.getWorkType().getId() : null)
                .workTypeName(entry.getWorkType() != null ? entry.getWorkType().getName() : null)
                .leaveTypeId(entry.getLeaveType() != null ? entry.getLeaveType().getId() : null)
                .leaveTypeName(entry.getLeaveType() != null ? entry.getLeaveType().getName() : null)
                .hours(scale(entry.getHours() != null ? entry.getHours() : BigDecimal.ZERO))
                .notes(entry.getNotes())
                .autoLeave(entry.getAutoLeave())
                .sourceLeaveRequestId(entry.getSourceLeaveRequestId())
                .build();
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
