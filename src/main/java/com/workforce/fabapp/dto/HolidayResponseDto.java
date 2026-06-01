package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class HolidayResponseDto {

    private Long id;
    private String name;
    private LocalDate date;
    private LocalDate observedDate;
    private String scope;
    private BigDecimal paidHours;
    private Boolean active;
}