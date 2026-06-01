package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayrollExportRowDto {

    private Long employeeId;
    private String employeeName;

    private Long jobId;
    private String jobNumber;
    private String jobName;
    private String xNumber;

    private BigDecimal regularHours;
    private BigDecimal otHours;
    private BigDecimal doubleTimeHours;
    private BigDecimal totalHours;
}