package com.workforce.fabapp.controller;

import com.workforce.fabapp.repository.*;
import com.workforce.fabapp.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class EmployeeTimeReportController {
    private final TimesheetEntryRepository entries;
    private final EmployeeRepository employees;
    private final UserRepository users;

    public record Line(Long entryId, LocalDate date, String jobNumber, String jobName,
                       String category, BigDecimal hours, String status, String notes) {}

    @GetMapping("/api/history/employee/{employeeId}/entries")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Transactional(readOnly = true)
    public List<Line> report(@PathVariable Long employeeId, @RequestParam LocalDate start,
                             @RequestParam LocalDate end, Authentication authentication) {
        if (end.isBefore(start)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must not precede start date");
        var user = users.findByUsername(authentication.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        var employee = employees.findById(employeeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (user.getRole() != Role.ADMIN && (user.getSupervisor() == null || employee.getSupervisor() == null
                || !user.getSupervisor().getId().equals(employee.getSupervisor().getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Employee is not in your team");
        }
        return entries.findEmployeeReport(employeeId, start, end).stream().map(e -> new Line(
                e.getId(), e.getWorkDate(), e.getJob() != null ? e.getJob().getCode() : e.getJobRequest() != null ? e.getJobRequest().getRequestedJobNumber() : "",
                e.getJob() != null ? e.getJob().getName() : "",
                e.getLeaveType() != null ? e.getLeaveType().getName() : e.getWorkType() != null ? e.getWorkType().getName() : "",
                e.getHours(), e.getTimesheetWeek().getStatus().name(), e.getNotes())).toList();
    }
}
