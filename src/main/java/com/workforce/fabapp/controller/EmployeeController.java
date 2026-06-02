package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.EmployeeSummaryDto;
import com.workforce.fabapp.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/{employeeId}")
    public EmployeeSummaryDto getEmployee(@PathVariable Long employeeId) {
        return employeeService.getEmployee(employeeId);
    }
}
