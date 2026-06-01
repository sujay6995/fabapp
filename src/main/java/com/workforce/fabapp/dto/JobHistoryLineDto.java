package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class JobHistoryLineDto {

    private Long entryId;

    private Long employeeId;
    private String employeeName;

    private LocalDate workDate;
    private LocalDate weekStart;

    private String workTypeName;

    private BigDecimal hours;
    private String notes;

    private String weekStatus;
}