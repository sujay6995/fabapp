package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.JobBudgetSummaryDto;
import com.workforce.fabapp.dto.UpdateJobBudgetDto;
import com.workforce.fabapp.dto.UpsertJobBudgetSplitDto;
import com.workforce.fabapp.service.JobBudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-budgets")
@RequiredArgsConstructor
public class JobBudgetController {

    private final JobBudgetService jobBudgetService;

    @GetMapping
    public List<JobBudgetSummaryDto> getJobSummaries(
            @RequestParam(required = false, defaultValue = "OPEN") String status
    ) {
        return jobBudgetService.getJobSummaries(status);
    }

    @GetMapping("/{jobId}")
    public JobBudgetSummaryDto getJobSummary(@PathVariable Long jobId) {
        return jobBudgetService.getJobSummary(jobId);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @PutMapping("/{jobId}")
    public JobBudgetSummaryDto updateJobBudget(
            @PathVariable Long jobId,
            @RequestBody UpdateJobBudgetDto dto
    ) {
        return jobBudgetService.updateJobBudget(jobId, dto);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @PutMapping("/{jobId}/category-split")
    public JobBudgetSummaryDto upsertBudgetSplit(
            @PathVariable Long jobId,
            @Valid @RequestBody UpsertJobBudgetSplitDto dto
    ) {
        return jobBudgetService.upsertBudgetSplit(jobId, dto);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @DeleteMapping("/{jobId}/category-split")
    public JobBudgetSummaryDto clearBudgetSplit(@PathVariable Long jobId) {
        return jobBudgetService.clearBudgetSplit(jobId);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @PostMapping("/{jobId}/budget-lock")
    public JobBudgetSummaryDto lockBudget(@PathVariable Long jobId) {
        return jobBudgetService.lockBudget(jobId);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @PostMapping("/{jobId}/budget-unlock")
    public JobBudgetSummaryDto unlockBudget(@PathVariable Long jobId) {
        return jobBudgetService.unlockBudget(jobId);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @PostMapping("/{jobId}/close")
    public JobBudgetSummaryDto closeJob(@PathVariable Long jobId) {
        return jobBudgetService.closeJob(jobId);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @PostMapping("/{jobId}/reopen")
    public JobBudgetSummaryDto reopenJob(@PathVariable Long jobId) {
        return jobBudgetService.reopenJob(jobId);
    }
}
