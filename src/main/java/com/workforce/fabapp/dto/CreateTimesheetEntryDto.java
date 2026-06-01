package com.workforce.fabapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateTimesheetEntryDto {

    @NotNull
    private Long timesheetWeekId;

    @NotNull
    private LocalDate workDate;

    private Long jobId;

    private Long workTypeId;

    private Long leaveTypeId;

    @NotNull
    @DecimalMin("0.1")
    private BigDecimal hours;

    private String notes;
}