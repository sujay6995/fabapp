package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class HistorySummaryDto {

    private LocalDate start;
    private LocalDate end;

    private long weekCount;
    private long employeeCount;

    private BigDecimal totalHours;
    private BigDecimal regularHours;
    private BigDecimal otHours;
    private BigDecimal leaveHours;

    private long submittedCount;
    private long approvedCount;
    private long payrollLockedCount;

    private List<HistoryWeekDto> weeks;
}