package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.CreateVacationPayRequestDto;
import com.workforce.fabapp.dto.VacationPayRequestResponseDto;
import com.workforce.fabapp.entity.Employee;
import com.workforce.fabapp.entity.VacationPayRequest;
import com.workforce.fabapp.enums.VacationPayStatus;
import com.workforce.fabapp.repository.EmployeeRepository;
import com.workforce.fabapp.repository.VacationPayRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VacationPayRequestService {

    private final VacationPayRequestRepository vacationPayRequestRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public VacationPayRequestResponseDto create(CreateVacationPayRequestDto dto) {
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("Vacation pay end date cannot be before start date.");
        }

        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        VacationPayRequest request = vacationPayRequestRepository.save(VacationPayRequest.builder()
                .employee(employee)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(VacationPayStatus.REQUESTED)
                .submittedAt(LocalDateTime.now())
                .build());

        return map(request);
    }

    @Transactional(readOnly = true)
    public List<VacationPayRequestResponseDto> getByEmployee(Long employeeId) {
        return vacationPayRequestRepository.findByEmployeeIdWithDetailsOrderBySubmittedAtDesc(employeeId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VacationPayRequestResponseDto> getOverlappingWeek(LocalDate weekStart) {
        LocalDate normalizedWeekStart = normalizeToSunday(weekStart);
        return getOverlapping(normalizedWeekStart, normalizedWeekStart.plusDays(6));
    }

    @Transactional(readOnly = true)
    public List<VacationPayRequestResponseDto> getOverlapping(LocalDate startDate, LocalDate endDate) {
        return vacationPayRequestRepository.findOverlappingWithDetails(startDate, endDate)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public VacationPayRequestResponseDto processed(Long requestId, String actor) {
        VacationPayRequest request = vacationPayRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Vacation pay request not found"));

        if (request.getStatus() == VacationPayStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled vacation pay requests cannot be processed.");
        }

        request.setStatus(VacationPayStatus.PROCESSED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(actor == null || actor.isBlank() ? "System" : actor);

        return map(vacationPayRequestRepository.save(request));
    }

    @Transactional
    public VacationPayRequestResponseDto cancel(Long requestId, String actor) {
        VacationPayRequest request = vacationPayRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Vacation pay request not found"));

        if (request.getStatus() == VacationPayStatus.PROCESSED) {
            throw new IllegalStateException("Processed vacation pay requests cannot be cancelled.");
        }

        request.setStatus(VacationPayStatus.CANCELLED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(actor == null || actor.isBlank() ? "System" : actor);

        return map(vacationPayRequestRepository.save(request));
    }

    private VacationPayRequestResponseDto map(VacationPayRequest request) {
        return VacationPayRequestResponseDto.builder()
                .id(request.getId())
                .employeeId(request.getEmployee().getId())
                .employeeName(request.getEmployee().getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus().name())
                .submittedAt(request.getSubmittedAt())
                .processedAt(request.getProcessedAt())
                .processedBy(request.getProcessedBy())
                .build();
    }

    private LocalDate normalizeToSunday(LocalDate date) {
        while (date.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
            date = date.minusDays(1);
        }
        return date;
    }
}
