package com.workforce.fabapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AdminUpsertJobDto {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    @NotNull
    private Boolean active;

    private List<Long> crewIds;
}