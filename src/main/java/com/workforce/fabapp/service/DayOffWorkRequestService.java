package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.DayOffWorkRequestDto;
import com.workforce.fabapp.dto.DayOffWorkResponseDto;
import com.workforce.fabapp.entity.AuditLog;
import com.workforce.fabapp.entity.DayOffWorkRequest;
import com.workforce.fabapp.entity.Employee;
import com.workforce.fabapp.enums.DayOffWorkRequestStatus;
import com.workforce.fabapp.repository.AuditLogRepository;
import com.workforce.fabapp.repository.DayOffWorkRequestRepository;
import com.workforce.fabapp.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DayOffWorkRequestService {

    private final DayOffWorkRequestRepository requestRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public DayOffWorkResponseDto create(DayOffWorkRequestDto dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        if (requestRepository.existsByEmployeeIdAndWorkDateAndStatusNot(
                employee.getId(), dto.getWorkDate(), DayOffWorkRequestStatus.REJECTED)) {
            throw new IllegalStateException("An active work request already exists for this date.");
        }

        DayOffWorkRequest request = requestRepository.save(DayOffWorkRequest.builder()
                .employee(employee)
                .supervisor(employee.getSupervisor())
                .workDate(dto.getWorkDate())
                .requestedHours(dto.getRequestedHours())
                .note(dto.getNote().trim())
                .status(DayOffWorkRequestStatus.PENDING_SUPERVISOR)
                .submittedAt(LocalDateTime.now())
                .build());

        auditLogRepository.save(AuditLog.builder()
                .actor(employee.getName())
                .item("Scheduled-day-off work requested for " + dto.getWorkDate())
                .at(LocalDateTime.now())
                .build());
        return map(request);
    }

    @Transactional(readOnly = true)
    public List<DayOffWorkResponseDto> getByEmployee(Long employeeId) {
        return requestRepository.findByEmployeeIdWithDetails(employeeId).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<DayOffWorkResponseDto> getBySupervisor(Long supervisorId) {
        return requestRepository.findBySupervisorIdWithDetails(supervisorId).stream().map(this::map).toList();
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "attendance"}, allEntries = true)
    public DayOffWorkResponseDto review(Long requestId, DayOffWorkRequestStatus decision, String actor) {
        DayOffWorkRequest request = requestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Scheduled-day-off work request not found"));
        if (request.getStatus() != DayOffWorkRequestStatus.PENDING_SUPERVISOR) {
            throw new IllegalStateException("Only pending work requests can be reviewed.");
        }

        String reviewedBy = actor == null || actor.isBlank() ? "System" : actor;
        request.setStatus(decision);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(reviewedBy);
        request = requestRepository.save(request);

        auditLogRepository.save(AuditLog.builder()
                .actor(reviewedBy)
                .item("Scheduled-day-off work " + decision.name().toLowerCase() + " for " + request.getEmployee().getName())
                .at(LocalDateTime.now())
                .build());
        return map(request);
    }

    private DayOffWorkResponseDto map(DayOffWorkRequest request) {
        return DayOffWorkResponseDto.builder()
                .id(request.getId())
                .employeeId(request.getEmployee().getId())
                .employeeName(request.getEmployee().getName())
                .supervisorId(request.getSupervisor().getId())
                .workDate(request.getWorkDate())
                .requestedHours(request.getRequestedHours())
                .note(request.getNote())
                .status(request.getStatus().name())
                .submittedAt(request.getSubmittedAt())
                .reviewedAt(request.getReviewedAt())
                .reviewedBy(request.getReviewedBy())
                .version(request.getVersion())
                .build();
    }
}
