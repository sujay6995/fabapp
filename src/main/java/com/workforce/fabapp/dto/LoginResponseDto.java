package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponseDto {
    private Long userId;
    private String username;
    private String name;
    private String role;
    private Long employeeId;
    private Long supervisorId;
    private String title;
    private Boolean active;
    private String token;
    private String tokenType;
}