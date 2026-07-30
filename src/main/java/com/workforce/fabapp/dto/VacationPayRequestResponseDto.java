package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class VacationPayRequestResponseDto {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime processedAt;
    private String processedBy;
}
