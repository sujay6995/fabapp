package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.*;
import com.workforce.fabapp.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public AdminDashboardDto getDashboard() {
        return adminService.getDashboardSummary();
    }

    @GetMapping("/jobs")
    public List<AdminJobDto> getJobs() {
        return adminService.getJobs();
    }

    @PostMapping("/jobs")
    public AdminJobDto createJob(@Valid @RequestBody AdminUpsertJobDto dto) {
        return adminService.createJob(dto);
    }

    @PutMapping("/jobs/{jobId}")
    public AdminJobDto updateJob(@PathVariable Long jobId, @Valid @RequestBody AdminUpsertJobDto dto) {
        return adminService.updateJob(jobId, dto);
    }

    @GetMapping("/employees")
    public List<AdminEmployeeDto> getEmployees() {
        return adminService.getEmployees();
    }

    @PostMapping("/employees")
    public AdminEmployeeDto createEmployee(@Valid @RequestBody AdminUpsertEmployeeDto dto) {
        return adminService.createEmployee(dto);
    }

    @PutMapping("/employees/{employeeId}")
    public AdminEmployeeDto updateEmployee(@PathVariable Long employeeId, @Valid @RequestBody AdminUpsertEmployeeDto dto) {
        return adminService.updateEmployee(employeeId, dto);
    }

    @GetMapping("/users")
    public List<AdminUserDto> getUsers() {
        return adminService.getUsers();
    }

    @PostMapping("/users")
    public AdminUserDto createUser(@Valid @RequestBody AdminUpsertUserDto dto) {
        return adminService.createUser(dto);
    }

    @PutMapping("/users/{userId}")
    public AdminUserDto updateUser(@PathVariable Long userId, @Valid @RequestBody AdminUpsertUserDto dto) {
        return adminService.updateUser(userId, dto);
    }

    @DeleteMapping("/users/{userId}")
    public void deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
    }

    @GetMapping("/payroll-review")
    public List<PayrollReviewDto> getPayrollReview(@RequestParam(required = false) LocalDate weekStart) {
        return adminService.getPayrollReview(weekStart);
    }

    @PostMapping("/payroll-review/{weekId}/lock")
    public void lockWeek(@PathVariable Long weekId) {
        adminService.lockPayrollWeek(weekId);
    }

    @PostMapping("/payroll-review/{weekId}/unlock")
    public void unlockWeek(@PathVariable Long weekId) {
        adminService.unlockPayrollWeek(weekId);
    }

    @GetMapping("/supervisors")
    public List<AdminSupervisorDto> getSupervisors() {
        return adminService.getSupervisors();
    }

    @PostMapping("/supervisors")
    public AdminSupervisorDto createSupervisor(@Valid @RequestBody AdminUpsertSupervisorDto dto) {
        return adminService.createSupervisor(dto);
    }

    @PutMapping("/supervisors/{supervisorId}")
    public AdminSupervisorDto updateSupervisor(
            @PathVariable Long supervisorId,
            @Valid @RequestBody AdminUpsertSupervisorDto dto
    ) {
        return adminService.updateSupervisor(supervisorId, dto);
    }
}
