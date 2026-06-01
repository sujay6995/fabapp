package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ScheduleDayDto {
    private LocalDate date;
    private Boolean workday;
    private String code;
    private String label;
    private Boolean hasApprovedLeave;
    private String leaveTypeName;
}