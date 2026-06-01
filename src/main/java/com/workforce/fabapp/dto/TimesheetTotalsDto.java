package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TimesheetTotalsDto {
    private BigDecimal totalHours;
    private BigDecimal eligibleHours;
    private BigDecimal leaveHours;
    private BigDecimal regularHours;
    private BigDecimal otHours;
    private BigDecimal otPayFactor;
}