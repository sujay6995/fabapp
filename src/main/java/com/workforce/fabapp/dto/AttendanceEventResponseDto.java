package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AttendanceEventResponseDto {

    private Long id;

    private Long employeeId;
    private String employeeName;

    private LocalDate eventDate;
    private LocalDate weekStart;

    private String kind;
    private String details;

    private String source;

    private String createdBy;
    private LocalDateTime createdAt;

    private String updatedBy;
    private LocalDateTime updatedAt;
}