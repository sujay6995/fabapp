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

    private Boolean premiumPayEnabled;
    @jakarta.validation.constraints.Size(max = 120)
    private String premiumPayLabel;
    @jakarta.validation.constraints.DecimalMin("0.00")
    @jakarta.validation.constraints.Digits(integer = 8, fraction = 2)
    private java.math.BigDecimal premiumPayRate;
}
