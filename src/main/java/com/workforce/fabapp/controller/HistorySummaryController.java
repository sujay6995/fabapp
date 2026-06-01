package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.EmployeeHistoryDto;
import com.workforce.fabapp.dto.HistorySummaryDto;
import com.workforce.fabapp.dto.JobHistoryDto;
import com.workforce.fabapp.service.HistorySummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@CrossOrigin
@PreAuthorize("hasRole('ADMIN')")
public class HistorySummaryController {

    private final HistorySummaryService historySummaryService;

    @GetMapping("/summary")
    public HistorySummaryDto getSummary(
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end
    ) {
        return historySummaryService.getSummary(start, end);
    }

    @GetMapping("/employee/{employeeId}")
    public EmployeeHistoryDto getEmployeeHistory(
            @PathVariable Long employeeId,
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end
    ) {
        return historySummaryService.getEmployeeHistory(employeeId, start, end);
    }

    @GetMapping("/job/{jobId}")
    public JobHistoryDto getJobHistory(@PathVariable Long jobId) {
        return historySummaryService.getJobHistory(jobId);
    }
}