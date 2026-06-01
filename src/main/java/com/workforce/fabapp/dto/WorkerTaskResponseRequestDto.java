package com.workforce.fabapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkerTaskResponseRequestDto {

    @NotBlank
    private String response;
}