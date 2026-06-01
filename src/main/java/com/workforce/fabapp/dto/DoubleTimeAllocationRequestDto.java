package com.workforce.fabapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DoubleTimeAllocationRequestDto {

    @NotNull
    private Long timesheetWeekId;

    private Long timesheetEntryId;

    private Long jobId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal hours;

    private String note;

    private String createdBy;
}