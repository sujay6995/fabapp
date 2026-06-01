package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminJobDto {
    private Long id;
    private String code;
    private String name;
    private Boolean active;
    private List<Long> crewIds;
    private List<String> crewNames;
}