package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DoubleTimeAllocationResponseDto {

    private Long id;

    private Long timesheetWeekId;
    private Long timesheetEntryId;

    private Long employeeId;
    private String employeeName;

    private Long jobId;
    private String jobCode;
    private String jobName;

    private BigDecimal hours;
    private String note;
    private String status;

    private String createdBy;
    private LocalDateTime createdAt;

    private String removedBy;
    private LocalDateTime removedAt;
}