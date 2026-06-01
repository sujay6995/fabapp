package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class EmployeeHistoryDto {

    private Long employeeId;
    private String employeeName;

    private String departmentName;
    private String crewName;
    private String supervisorName;

    private BigDecimal totalHours;
    private BigDecimal regularHours;
    private BigDecimal otHours;
    private BigDecimal leaveHours;

    private long submittedWeeks;
    private long approvedWeeks;
    private long payrollLockedWeeks;

    private List<HistoryWeekDto> weeks;
}