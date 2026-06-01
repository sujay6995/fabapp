package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmployeeSummaryDto {
    private Long id;
    private String employeeCode;
    private String name;
    private String departmentName;
    private String crewName;
    private Long supervisorId;
    private String supervisorName;
    private Integer weeklyTargetHours;
    private String shiftPatternName;
}