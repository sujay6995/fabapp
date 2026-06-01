package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OvertimeAllocationResponseDto {

    private Long id;
    private Long timesheetWeekId;
    private Long jobId;
    private String jobCode;
    private String jobName;
    private BigDecimal hours;
    private String note;
    private Integer sortOrder;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
