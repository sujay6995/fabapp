package com.workforce.fabapp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpsertJobBudgetSplitDto {

    private Boolean budgetLocked;

    private List<CategoryBudgetLine> categories;

    @Data
    public static class CategoryBudgetLine {
        private Long workTypeId;
        private BigDecimal budgetHours;
    }
}
