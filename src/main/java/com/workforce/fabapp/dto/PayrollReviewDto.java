package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PayrollReviewDto {
    private Long weekId;
    private Long employeeId;
    private String employeeName;
    private LocalDate weekStart;
    private String status;
    private Boolean payrollLocked;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private Long supervisorId;
    private String supervisorName;
}