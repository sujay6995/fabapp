package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PayrollExportResponseDto {

    private LocalDate weekStart;
    private LocalDate weekEnd;

    private BigDecimal regularHours;
    private BigDecimal otHours;
    private BigDecimal doubleTimeHours;
    private BigDecimal totalHours;

    private boolean exportBlocked;
    private List<String> blockingReasons;

    private List<PayrollExportRowDto> rows;
}