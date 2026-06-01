package com.workforce.fabapp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OvertimeAllocationRequestDto {

    private Long jobId;

    @NotNull
    private BigDecimal hours;

    private String note;
}
