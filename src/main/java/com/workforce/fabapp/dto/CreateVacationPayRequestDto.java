package com.workforce.fabapp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateVacationPayRequestDto {

    @NotNull
    private Long employeeId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}
