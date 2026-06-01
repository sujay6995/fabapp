package com.workforce.fabapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUpsertSupervisorDto {

    @NotBlank
    private String supervisorCode;

    @NotBlank
    private String name;

    private String title;

    @NotNull
    private Boolean active;
}