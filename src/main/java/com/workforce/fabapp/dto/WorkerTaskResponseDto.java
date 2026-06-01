package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class WorkerTaskResponseDto {
    private Long id;
    private String title;
    private String description;
    private LocalDate dueDate;
    private String status;

    private Long employeeId;
    private String employeeName;

    private Long supervisorId;
    private String supervisorName;

    private Long jobId;
    private String jobCode;
    private String jobName;

    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime seenAt;
    private String response;
    private LocalDateTime respondedAt;
    private LocalDateTime completedAt;
}