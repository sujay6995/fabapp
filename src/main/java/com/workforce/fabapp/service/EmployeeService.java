package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.EmployeeSummaryDto;
import com.workforce.fabapp.entity.Employee;
import com.workforce.fabapp.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeSummaryDto getEmployee(Long employeeId) {
        Employee e = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        return map(e);
    }

    public List<EmployeeSummaryDto> getActiveEmployees() {
        return employeeRepository.findAllActiveWithProfile().stream().map(this::map).toList();
    }

    private EmployeeSummaryDto map(Employee e) {
        return EmployeeSummaryDto.builder()
                .id(e.getId())
                .employeeCode(e.getEmployeeCode())
                .name(e.getName())
                .departmentName(e.getDepartment().getName())
                .crewName(e.getCrew().getName())
                .supervisorId(e.getSupervisor().getId())
                .supervisorName(e.getSupervisor().getName())
                .weeklyTargetHours(e.getWeeklyTargetHours())
                .shiftPatternName(e.getShiftPatternName())
                .build();
    }
}
