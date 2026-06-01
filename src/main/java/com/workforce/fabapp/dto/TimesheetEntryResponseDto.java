package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class TimesheetEntryResponseDto {
    private Long id;
    private LocalDate workDate;
    private Long jobId;
    private String jobCode;
    private String jobName;
    private Long workTypeId;
    private String workTypeName;
    private Long leaveTypeId;
    private String leaveTypeName;
    private BigDecimal hours;
    private String notes;
    private Boolean autoLeave;
    private Long sourceLeaveRequestId;
    private Long jobRequestId;
    private String pendingJobNumber;
    private String pendingJobStatus;
}