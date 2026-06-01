package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.AdminSupervisorDto;
import com.workforce.fabapp.dto.EmployeeSummaryDto;
import com.workforce.fabapp.entity.Employee;
import com.workforce.fabapp.entity.Supervisor;
import com.workforce.fabapp.repository.EmployeeRepository;
import com.workforce.fabapp.repository.SupervisorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupervisorService {

    private final EmployeeRepository employeeRepository;
    private final SupervisorRepository supervisorRepository;

    public List<AdminSupervisorDto> getActiveSupervisors() {
        return supervisorRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::mapSupervisor)
                .toList();
    }

    public List<EmployeeSummaryDto> getTeamEmployees(Long supervisorId) {
        List<Employee> employees = employeeRepository.findBySupervisorIdAndActiveTrueWithProfile(supervisorId);

        return employees.stream()
                .map(e -> EmployeeSummaryDto.builder()
                        .id(e.getId())
                        .employeeCode(e.getEmployeeCode())
                        .name(e.getName())
                        .departmentName(e.getDepartment().getName())
                        .crewName(e.getCrew().getName())
                        .supervisorId(e.getSupervisor().getId())
                        .supervisorName(e.getSupervisor().getName())
                        .weeklyTargetHours(e.getWeeklyTargetHours())
                        .shiftPatternName(e.getShiftPatternName())
                        .build())
                .toList();
    }

    private AdminSupervisorDto mapSupervisor(Supervisor supervisor) {
        return AdminSupervisorDto.builder()
                .id(supervisor.getId())
                .supervisorCode(supervisor.getSupervisorCode())
                .name(supervisor.getName())
                .title(supervisor.getTitle())
                .active(supervisor.getActive())
                .build();
    }
}
