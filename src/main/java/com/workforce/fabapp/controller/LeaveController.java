package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.CreateLeaveRequestDto;
import com.workforce.fabapp.dto.LeaveRequestResponseDto;
import com.workforce.fabapp.service.LeaveRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveRequestService leaveRequestService;

    @GetMapping("/approved")
    public List<LeaveRequestResponseDto> getApproved(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end
    ) {
        return leaveRequestService.getApproved(start, end);
    }

    @PostMapping
    public LeaveRequestResponseDto create(@Valid @RequestBody CreateLeaveRequestDto dto) {
        return leaveRequestService.create(dto);
    }

    @GetMapping("/employee/{employeeId}")
    public List<LeaveRequestResponseDto> getByEmployee(@PathVariable Long employeeId) {
        return leaveRequestService.getByEmployee(employeeId);
    }

    @GetMapping("/pending/{supervisorId}")
    public List<LeaveRequestResponseDto> getPending(@PathVariable Long supervisorId) {
        return leaveRequestService.getPendingBySupervisor(supervisorId);
    }

    @GetMapping("/supervisor/{supervisorId}/approved")
    public List<LeaveRequestResponseDto> getApproved(
            @PathVariable Long supervisorId,
            @RequestParam LocalDate start,
            @RequestParam LocalDate end
    ) {
        return leaveRequestService.getApprovedBySupervisor(supervisorId, start, end);
    }
}
