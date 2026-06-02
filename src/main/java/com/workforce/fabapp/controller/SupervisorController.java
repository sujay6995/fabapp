package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.AdminSupervisorDto;
import com.workforce.fabapp.dto.EmployeeSummaryDto;
import com.workforce.fabapp.service.SupervisorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
@RestController
@RequestMapping("/api/supervisors")
@RequiredArgsConstructor
public class SupervisorController {

    private final SupervisorService supervisorService;

    @GetMapping
    public List<AdminSupervisorDto> getSupervisors() {
        return supervisorService.getActiveSupervisors();
    }

    @GetMapping("/{supervisorId}/employees")
    public List<EmployeeSummaryDto> getTeamEmployees(@PathVariable Long supervisorId) {
        return supervisorService.getTeamEmployees(supervisorId);
    }
}
