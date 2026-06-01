package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class JobRequestResponseDto {
    private Long id;
    private String requestedJobNumber;
    private String xNumber;
    private String jobName;
    private String category;
    private BigDecimal totalBudgetHours;
    private Integer warningPercent;
    private String reason;
    private String status;
    private Long employeeId;
    private String employeeName;
    private Long supervisorId;
    private String supervisorName;
    private Long openedJobId;
    private String openedJobCode;
    private LocalDateTime createdAt;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNote;
}