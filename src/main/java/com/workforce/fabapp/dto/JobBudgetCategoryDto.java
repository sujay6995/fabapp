package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class JobBudgetCategoryDto {

    private Long id;

    private Long workTypeId;
    private String workTypeName;

    private BigDecimal budgetHours;
    private BigDecimal actualHours;
    private BigDecimal remainingHours;
    private BigDecimal usedPercent;
}