package com.workforce.fabapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUpsertEmployeeDto {

    @NotBlank
    private String employeeCode;

    @NotBlank
    private String name;

    @NotNull
    private Long departmentId;

    @NotNull
    private Long crewId;

    @NotNull
    private Long supervisorId;

    private String roleLabel;
    private String shiftPatternName;

    @NotNull
    private Integer weeklyTargetHours;

    @NotNull
    private Boolean active;
}