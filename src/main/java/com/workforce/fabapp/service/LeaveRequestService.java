package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.CreateLeaveRequestDto;
import com.workforce.fabapp.dto.LeaveRequestResponseDto;
import com.workforce.fabapp.entity.Employee;
import com.workforce.fabapp.entity.AuditLog;
import com.workforce.fabapp.entity.LeaveRequest;
import com.workforce.fabapp.entity.LeaveType;
import com.workforce.fabapp.enums.LeaveStatus;
import com.workforce.fabapp.repository.EmployeeRepository;
import com.workforce.fabapp.repository.AuditLogRepository;
import com.workforce.fabapp.repository.LeaveRequestRepository;
import com.workforce.fabapp.repository.LeaveTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final AuditLogRepository auditLogRepository;

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

    public List<LeaveRequestResponseDto> getApprovedBySupervisor(
            Long supervisorId,
            LocalDate start,
            LocalDate end
    ) {
        return leaveRequestRepository.findByApproverIdAndStatusOverlapping(
                        supervisorId,
                        LeaveStatus.APPROVED,
                        start,
                        end
                )
                .stream()
                .map(this::map)
                .toList();
    }

    public List<LeaveRequestResponseDto> getApproved(LocalDate start, LocalDate end) {
        return leaveRequestRepository.findByStatusOverlapping(LeaveStatus.APPROVED, start, end)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "attendance"}, allEntries = true)
    public void deletePending(Long leaveRequestId, String actor) {
        LeaveRequest leave = leaveRequestRepository.findByIdWithDetails(leaveRequestId)
                .orElseThrow(() -> new EntityNotFoundException("Leave request not found"));

        if (leave.getStatus() != LeaveStatus.PENDING_SUPERVISOR) {
            throw new IllegalStateException("Only pending leave requests can be removed.");
        }

        String actedBy = actor != null && !actor.isBlank() ? actor : "System";
        String employeeName = leave.getEmployee().getName();
        leaveRequestRepository.delete(leave);

        auditLogRepository.save(AuditLog.builder()
                .actor(actedBy)
                .item("Pending leave request removed for " + employeeName)
                .at(LocalDateTime.now())
                .build());
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
