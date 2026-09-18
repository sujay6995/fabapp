package com.workforce.fabapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CorrectJobNumberDto(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 500) String reason) {}
