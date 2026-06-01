package com.workforce.fabapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateWorkerTaskDto {

    @NotNull
    private Long employeeId;

    private Long jobId;

    @NotBlank
    private String title;

    private String description;

    private LocalDate dueDate;

    private Long supervisorId;

    private String createdBy;
}