package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.DayOffWorkRequestDto;
import com.workforce.fabapp.dto.DayOffWorkResponseDto;
import com.workforce.fabapp.dto.DayOffWorkReviewRequestDto;
import com.workforce.fabapp.enums.DayOffWorkRequestStatus;
import com.workforce.fabapp.service.DayOffWorkRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedule/day-off-work-requests")
@RequiredArgsConstructor
public class DayOffWorkRequestController {

    private final DayOffWorkRequestService requestService;

    @PostMapping
    public DayOffWorkResponseDto create(@Valid @RequestBody DayOffWorkRequestDto dto) {
        return requestService.create(dto);
    }

    @GetMapping("/employee/{employeeId}")
    public List<DayOffWorkResponseDto> getByEmployee(@PathVariable Long employeeId) {
        return requestService.getByEmployee(employeeId);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @GetMapping("/supervisor/{supervisorId}")
    public List<DayOffWorkResponseDto> getBySupervisor(@PathVariable Long supervisorId) {
        return requestService.getBySupervisor(supervisorId);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @PostMapping("/{requestId}/approve")
    public DayOffWorkResponseDto approve(
            @PathVariable Long requestId,
            @RequestBody(required = false) DayOffWorkReviewRequestDto dto
    ) {
        return requestService.review(requestId, DayOffWorkRequestStatus.APPROVED, dto != null ? dto.getActor() : "System");
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @PostMapping("/{requestId}/reject")
    public DayOffWorkResponseDto reject(
            @PathVariable Long requestId,
            @RequestBody(required = false) DayOffWorkReviewRequestDto dto
    ) {
        return requestService.review(requestId, DayOffWorkRequestStatus.REJECTED, dto != null ? dto.getActor() : "System");
    }
}
