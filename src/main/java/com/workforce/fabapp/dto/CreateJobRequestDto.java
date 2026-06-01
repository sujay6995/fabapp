package com.workforce.fabapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateJobRequestDto {

    @NotNull
    private Long employeeId;

    @NotBlank
    private String requestedJobNumber;

    private String xNumber;

    private String jobName;

    private String category;

    private BigDecimal totalBudgetHours;

    private Integer warningPercent;

    private String reason;
}