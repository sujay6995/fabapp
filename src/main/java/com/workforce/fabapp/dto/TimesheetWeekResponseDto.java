package com.workforce.fabapp.dto;

import com.workforce.fabapp.enums.TimesheetStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TimesheetWeekResponseDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private LocalDate weekStart;
    private TimesheetStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private Boolean payrollLocked;
    private Long supervisorId;
    private String supervisorName;
    private List<TimesheetEntryResponseDto> entries;
    private List<TimesheetIssueDto> issues;
    private TimesheetTotalsDto totals;
}