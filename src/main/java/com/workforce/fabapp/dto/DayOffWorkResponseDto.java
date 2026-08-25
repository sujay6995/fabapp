package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DayOffWorkResponseDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long supervisorId;
    private LocalDate workDate;
    private BigDecimal requestedHours;
    private String note;
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private String reviewedBy;
    private Long version;
}
