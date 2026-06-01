package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class HistoryWeekDto {

    private Long weekId;

    private Long employeeId;
    private String employeeName;

    private LocalDate weekStart;
    private LocalDate weekEnd;

    private String status;
    private Boolean payrollLocked;

    private BigDecimal totalHours;
    private BigDecimal regularHours;
    private BigDecimal otHours;
    private BigDecimal leaveHours;

    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;

    private Long supervisorId;
    private String supervisorName;
}