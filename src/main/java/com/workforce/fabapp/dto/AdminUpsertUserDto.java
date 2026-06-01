package com.workforce.fabapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUpsertUserDto {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    private String title;

    @NotBlank
    private String role;

    @NotNull
    private Boolean active;

    private Long employeeId;
    private Long supervisorId;
}