package com.workforce.fabapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SystemResetRequestDto {

    @NotBlank
    private String actor;

    @NotBlank
    private String confirmation;
}