package com.workforce.fabapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReviewJobRequestDto {

    @NotBlank
    private String reviewedBy;

    private String reviewNote;

    private String jobName;

    private String xNumber;

    private String category;

    private BigDecimal totalBudgetHours;

    private Integer warningPercent;

    private Boolean budgetLocked;
}