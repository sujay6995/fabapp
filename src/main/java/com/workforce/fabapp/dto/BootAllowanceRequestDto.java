package com.workforce.fabapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BootAllowanceRequestDto {

    @NotNull
    private Long employeeId;

    @NotNull
    private LocalDate purchaseDate;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private String note;

    @NotBlank
    private String receiptFileName;

    @NotBlank
    private String receiptStorageKey;
}
