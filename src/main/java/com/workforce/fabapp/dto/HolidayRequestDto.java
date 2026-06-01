package com.workforce.fabapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class HolidayRequestDto {

    @NotBlank
    private String name;

    @NotNull
    private LocalDate date;

    @NotNull
    private LocalDate observedDate;

    @NotBlank
    private String scope;

    private BigDecimal paidHours;

    @NotNull
    private Boolean active;
}