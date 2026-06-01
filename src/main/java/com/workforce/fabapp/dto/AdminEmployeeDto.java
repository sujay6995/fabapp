package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminEmployeeDto {
    private Long id;
    private String employeeCode;
    private String name;
    private Long departmentId;
    private String departmentName;
    private Long crewId;
    private String crewName;
    private Long supervisorId;
    private String supervisorName;
    private String roleLabel;
    private String shiftPatternName;
    private Integer weeklyTargetHours;
    private Boolean active;
}