package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.CreateLeaveRequestDto;
import com.workforce.fabapp.dto.LeaveRequestResponseDto;
import com.workforce.fabapp.service.LeaveRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
@CrossOrigin
public class LeaveController {

    private final LeaveRequestService leaveRequestService;

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
}