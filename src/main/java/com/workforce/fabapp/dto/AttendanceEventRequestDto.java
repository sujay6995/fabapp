package com.workforce.fabapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceEventRequestDto {

    @NotNull
    private Long employeeId;

    @NotNull
    private LocalDate eventDate;

    @NotBlank
    private String kind;

    private String details;

    private String source;

    private String actor;
}