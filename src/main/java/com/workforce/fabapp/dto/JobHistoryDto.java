package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class JobHistoryDto {

    private Long jobId;
    private String jobNumber;
    private String jobName;
    private String xNumber;
    private String category;

    private Boolean active;
    private Boolean closed;

    private BigDecimal totalBudgetHours;
    private Integer warningPercent;
    private Boolean budgetLocked;

    private BigDecimal actualHours;
    private BigDecimal regularHours;
    private BigDecimal otHours;
    private BigDecimal doubleTimeHours;

    private BigDecimal budgetRemainingHours;
    private BigDecimal budgetUsedPercent;

    private List<JobHistoryLineDto> lines;
}