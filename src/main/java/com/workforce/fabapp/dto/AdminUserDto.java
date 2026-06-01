package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserDto {
    private Long id;
    private String username;
    private String password;
    private String name;
    private String title;
    private String role;
    private Boolean active;
    private Long employeeId;
    private Long supervisorId;
}