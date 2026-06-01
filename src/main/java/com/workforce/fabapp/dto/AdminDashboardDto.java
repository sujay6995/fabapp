package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardDto {
    private long activeEmployees;
    private long activeUsers;
    private long activeJobs;
    private long submittedWeeks;
    private long approvedWeeks;
    private long pendingLeaveRequests;
}