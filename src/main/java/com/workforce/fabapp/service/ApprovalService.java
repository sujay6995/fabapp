package com.workforce.fabapp.service;

import com.workforce.fabapp.entity.*;
import com.workforce.fabapp.enums.JobRequestStatus;
import com.workforce.fabapp.enums.LeaveStatus;
import com.workforce.fabapp.enums.TimesheetStatus;
import com.workforce.fabapp.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final TimesheetWeekRepository timesheetWeekRepository;
    private final TimesheetEntryRepository timesheetEntryRepository;
    private final CrewScheduleRepository crewScheduleRepository;
    private final ApprovalActionRepository approvalActionRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "attendance", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public void approveLeave(Long leaveRequestId, String actedBy, String note) {
        LeaveRequest leave = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new EntityNotFoundException("Leave request not found"));

        if (leave.getStatus() == LeaveStatus.APPROVED) {
            return;
        }

        leave.setStatus(LeaveStatus.APPROVED);
        leave.setAppliedToSchedule(Boolean.FALSE);
        leaveRequestRepository.save(leave);

        syncApprovedLeaveIntoWeeks(leave);

        approvalActionRepository.save(ApprovalAction.builder()
                .recordType("Leave")
                .recordId(leave.getId())
                .action("Approved")
                .actedBy(actedBy)
                .actedAt(LocalDateTime.now())
                .note(note)
                .build());

        auditLogRepository.save(AuditLog.builder()
                .actor(actedBy)
                .item("Leave approved for " + leave.getEmployee().getName())
                .at(LocalDateTime.now())
                .build());
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "attendance", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public void rejectLeave(Long leaveRequestId, String actedBy, String note) {
        LeaveRequest leave = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new EntityNotFoundException("Leave request not found"));

        if (leave.getStatus() == LeaveStatus.APPROVED) {
            timesheetEntryRepository.deleteBySourceLeaveRequestId(leave.getId());
        }

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setAppliedToSchedule(Boolean.FALSE);
        leaveRequestRepository.save(leave);

        approvalActionRepository.save(ApprovalAction.builder()
                .recordType("Leave")
                .recordId(leave.getId())
                .action("Rejected")
                .actedBy(actedBy)
                .actedAt(LocalDateTime.now())
                .note(note)
                .build());

        auditLogRepository.save(AuditLog.builder()
                .actor(actedBy)
                .item("Leave rejected for " + leave.getEmployee().getName())
                .at(LocalDateTime.now())
                .build());
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "attendance", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public void approveTimesheet(Long weekId, String actedBy, String note) {
        TimesheetWeek week = timesheetWeekRepository.findById(weekId)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet week not found"));

        if (Boolean.TRUE.equals(week.getPayrollLocked()) || week.getStatus() == TimesheetStatus.PAYROLL_LOCKED) {
            throw new IllegalStateException("Week is payroll locked.");
        }

        if (week.getStatus() != TimesheetStatus.SUBMITTED
                && week.getStatus() != TimesheetStatus.REOPENED
                && week.getStatus() != TimesheetStatus.SENT_BACK) {
            throw new IllegalStateException("Only submitted/reopened/sent-back weeks can be approved.");
        }

        boolean hasUnresolvedJobRequests = timesheetEntryRepository
                .findByTimesheetWeekId(week.getId())
                .stream()
                .anyMatch(entry ->
                        entry.getJobRequest() != null
                                && entry.getJobRequest().getStatus() == JobRequestStatus.PENDING
                );

        if (hasUnresolvedJobRequests) {
            throw new IllegalStateException("Timesheet has unresolved pending job requests.");
        }

        week.setStatus(TimesheetStatus.APPROVED);
        week.setApprovedAt(LocalDateTime.now());
        timesheetWeekRepository.save(week);

        approvalActionRepository.save(ApprovalAction.builder()
                .recordType("Timesheet")
                .recordId(week.getId())
                .action("Approved")
                .actedBy(actedBy)
                .actedAt(LocalDateTime.now())
                .note(note)
                .build());

        auditLogRepository.save(AuditLog.builder()
                .actor(actedBy)
                .item(week.getEmployee().getName() + " week approved")
                .at(LocalDateTime.now())
                .build());
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "attendance", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public void sendBackTimesheet(Long weekId, String actedBy, String note) {
        TimesheetWeek week = timesheetWeekRepository.findById(weekId)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet week not found"));

        if (Boolean.TRUE.equals(week.getPayrollLocked()) || week.getStatus() == TimesheetStatus.PAYROLL_LOCKED) {
            throw new IllegalStateException("Week is payroll locked.");
        }

        if (week.getStatus() != TimesheetStatus.SUBMITTED
                && week.getStatus() != TimesheetStatus.REOPENED) {
            throw new IllegalStateException("Only submitted/reopened weeks can be sent back.");
        }

        week.setStatus(TimesheetStatus.SENT_BACK);
        timesheetWeekRepository.save(week);

        approvalActionRepository.save(ApprovalAction.builder()
                .recordType("Timesheet")
                .recordId(week.getId())
                .action("Sent Back")
                .actedBy(actedBy)
                .actedAt(LocalDateTime.now())
                .note(note)
                .build());

        auditLogRepository.save(AuditLog.builder()
                .actor(actedBy)
                .item(week.getEmployee().getName() + " week sent back")
                .at(LocalDateTime.now())
                .build());
    }

    private void syncApprovedLeaveIntoWeeks(LeaveRequest leave) {
        timesheetEntryRepository.deleteBySourceLeaveRequestId(leave.getId());

        Employee employee = leave.getEmployee();

        List<LocalDate> allDates = leave.getStartDate()
                .datesUntil(leave.getEndDate().plusDays(1))
                .toList();

        List<LocalDate> scheduledDates = allDates.stream()
                .filter(date -> crewScheduleRepository
                        .findByCrewIdAndWorkDate(employee.getCrew().getId(), date)
                        .map(CrewSchedule::getIsWorkday)
                        .orElse(false))
                .toList();

        BigDecimal dailyHours = BigDecimal.ZERO;
        if (!scheduledDates.isEmpty() && leave.getHours() != null) {
            dailyHours = leave.getHours()
                    .divide(BigDecimal.valueOf(scheduledDates.size()), 2, RoundingMode.HALF_UP);
        }

        for (LocalDate date : scheduledDates) {
            LocalDate weekStart = normalizeToSunday(date);

            TimesheetWeek week = timesheetWeekRepository
                    .findByEmployeeIdAndWeekStart(employee.getId(), weekStart)
                    .orElseGet(() -> createWeek(employee, weekStart));

            boolean exists = timesheetEntryRepository
                    .findByTimesheetWeekIdAndWorkDate(week.getId(), date)
                    .stream()
                    .anyMatch(entry ->
                            Boolean.TRUE.equals(entry.getAutoLeave())
                                    && leave.getId().equals(entry.getSourceLeaveRequestId())
                                    && date.equals(entry.getWorkDate())
                    );

            if (!exists) {
                TimesheetEntry entry = TimesheetEntry.builder()
                        .timesheetWeek(week)
                        .workDate(date)
                        .job(null)
                        .jobRequest(null)
                        .workType(null)
                        .leaveType(leave.getLeaveType())
                        .hours(dailyHours)
                        .notes("Approved " + leave.getLeaveType().getName().toLowerCase())
                        .autoLeave(Boolean.TRUE)
                        .sourceLeaveRequestId(leave.getId())
                        .build();

                timesheetEntryRepository.save(entry);
            }
        }

        leave.setAppliedToSchedule(Boolean.TRUE);
        leaveRequestRepository.save(leave);
    }

    private TimesheetWeek createWeek(Employee employee, LocalDate weekStart) {
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
}
