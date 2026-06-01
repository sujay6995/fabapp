package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TimesheetIssueDto {
    private String type;
    private LocalDate date;
    private String message;
}