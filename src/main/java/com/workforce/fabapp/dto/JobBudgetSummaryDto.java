package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class JobBudgetSummaryDto {

    private Long jobId;
    private String jobNumber;
    private String jobName;
    private String xNumber;
    private String category;

    private Boolean active;
    private Boolean closed;

    private BigDecimal totalBudgetHours;
    private BigDecimal actualHours;
    private BigDecimal remainingHours;
    private BigDecimal usedPercent;

    private Integer warningPercent;
    private Boolean budgetLocked;

    private String budgetStatus;

    private List<JobBudgetCategoryDto> categoryBudgets;
}