package com.workforce.fabapp.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateJobBudgetDto {
    private String jobName;
    private String xNumber;
    private String category;
    private BigDecimal totalBudgetHours;
    private Integer warningPercent;
    private Boolean budgetLocked;
}
