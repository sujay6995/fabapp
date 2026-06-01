package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.CreateLeaveRequestDto;
import com.workforce.fabapp.dto.LeaveRequestResponseDto;
import com.workforce.fabapp.entity.Employee;
import com.workforce.fabapp.entity.LeaveRequest;
import com.workforce.fabapp.entity.LeaveType;
import com.workforce.fabapp.enums.LeaveStatus;
import com.workforce.fabapp.repository.EmployeeRepository;
import com.workforce.fabapp.repository.LeaveRequestRepository;
import com.workforce.fabapp.repository.LeaveTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveRequestResponseDto create(CreateLeaveRequestDto dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        LeaveType leaveType = leaveTypeRepository.findById(dto.getLeaveTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Leave type not found"));

        LeaveRequest entity = LeaveRequest.builder()
                .employee(employee)
                .leaveType(leaveType)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .hours(dto.getHours())
                .status(LeaveStatus.PENDING_SUPERVISOR)
                .approver(employee.getSupervisor())
                .notes(dto.getNotes())
                .appliedToSchedule(Boolean.FALSE)
                .build();

        entity = leaveRequestRepository.save(entity);
        return map(entity);
    }

    public List<LeaveRequestResponseDto> getByEmployee(Long employeeId) {
        return leaveRequestRepository.findByEmployeeIdOrderByStartDateDesc(employeeId)
                .stream()
                .map(this::map)
                .toList();
    }

    public List<LeaveRequestResponseDto> getPendingBySupervisor(Long supervisorId) {
        return leaveRequestRepository.findByApproverIdAndStatus(supervisorId, LeaveStatus.PENDING_SUPERVISOR)
                .stream()
                .map(this::map)
                .toList();
    }

    private LeaveRequestResponseDto map(LeaveRequest lr) {
        return LeaveRequestResponseDto.builder()
                .id(lr.getId())
                .employeeId(lr.getEmployee().getId())
                .employeeName(lr.getEmployee().getName())
                .leaveTypeId(lr.getLeaveType().getId())
                .leaveTypeName(lr.getLeaveType().getName())
                .startDate(lr.getStartDate())
                .endDate(lr.getEndDate())
                .hours(lr.getHours())
                .status(lr.getStatus().name())
                .approverId(lr.getApprover() != null ? lr.getApprover().getId() : null)
                .approverName(lr.getApprover() != null ? lr.getApprover().getName() : null)
                .notes(lr.getNotes())
                .appliedToSchedule(lr.getAppliedToSchedule())
                .build();
    }
}