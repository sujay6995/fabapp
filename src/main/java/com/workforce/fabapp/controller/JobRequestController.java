package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.CreateJobRequestDto;
import com.workforce.fabapp.dto.JobRequestResponseDto;
import com.workforce.fabapp.dto.ReviewJobRequestDto;
import com.workforce.fabapp.service.JobRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-requests")
@RequiredArgsConstructor
public class JobRequestController {

    private final JobRequestService jobRequestService;

    @PostMapping
    public JobRequestResponseDto create(@Valid @RequestBody CreateJobRequestDto dto) {
        return jobRequestService.create(dto);
    }

    @GetMapping("/employee/{employeeId}")
    public List<JobRequestResponseDto> getByEmployee(@PathVariable Long employeeId) {
        return jobRequestService.getByEmployee(employeeId);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @GetMapping("/supervisor/{supervisorId}/pending")
    public List<JobRequestResponseDto> getPendingBySupervisor(@PathVariable Long supervisorId) {
        return jobRequestService.getPendingBySupervisor(supervisorId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending")
    public List<JobRequestResponseDto> getAllPending() {
        return jobRequestService.getAllPending();
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @PostMapping("/{requestId}/approve-open")
    public JobRequestResponseDto approveAndOpen(
            @PathVariable Long requestId,
            @Valid @RequestBody ReviewJobRequestDto dto
    ) {
        return jobRequestService.approveAndOpen(requestId, dto);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @PostMapping("/{requestId}/reject")
    public JobRequestResponseDto reject(
            @PathVariable Long requestId,
            @Valid @RequestBody ReviewJobRequestDto dto
    ) {
        return jobRequestService.reject(requestId, dto);
    }
}
