package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminSupervisorDto {
    private Long id;
    private String supervisorCode;
    private String name;
    private String title;
    private Boolean active;
}