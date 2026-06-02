package com.workforce.fabapp.controller;

import com.workforce.fabapp.service.ApprovalService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @PostMapping("/leave/{leaveRequestId}/approve")
    public void approveLeave(@PathVariable Long leaveRequestId, @RequestBody ActionRequest req) {
        approvalService.approveLeave(
                leaveRequestId,
                req.getActedBy() != null ? req.getActedBy() : "System",
                req.getNote()
        );
    }

    @PostMapping("/leave/{leaveRequestId}/reject")
    public void rejectLeave(@PathVariable Long leaveRequestId, @RequestBody ActionRequest req) {
        approvalService.rejectLeave(
                leaveRequestId,
                req.getActedBy() != null ? req.getActedBy() : "System",
                req.getNote()
        );
    }

    @PostMapping("/timesheet/{weekId}/approve")
    public void approveTimesheet(@PathVariable Long weekId, @RequestBody ActionRequest req) {
        approvalService.approveTimesheet(
                weekId,
                req.getActedBy() != null ? req.getActedBy() : "System",
                req.getNote()
        );
    }

    @PostMapping("/timesheet/{weekId}/send-back")
    public void sendBackTimesheet(@PathVariable Long weekId, @RequestBody ActionRequest req) {
        approvalService.sendBackTimesheet(
                weekId,
                req.getActedBy() != null ? req.getActedBy() : "System",
                req.getNote()
        );
    }

    @Data
    public static class ActionRequest {
        private String actedBy;
        private String note;
    }
}
