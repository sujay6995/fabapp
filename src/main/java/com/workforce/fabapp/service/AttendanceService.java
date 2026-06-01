package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.AttendanceEventRequestDto;
import com.workforce.fabapp.dto.AttendanceEventResponseDto;
import com.workforce.fabapp.entity.AttendanceEvent;
import com.workforce.fabapp.entity.Employee;
import com.workforce.fabapp.enums.AttendanceEventKind;
import com.workforce.fabapp.repository.AttendanceEventRepository;
import com.workforce.fabapp.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceEventRepository attendanceEventRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    @CacheEvict(value = {"attendance", "timesheetWeeks", "timesheetIssues"}, allEntries = true)
    public AttendanceEventResponseDto upsert(AttendanceEventRequestDto dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        AttendanceEventKind kind = parseKind(dto.getKind());
        LocalDate weekStart = normalizeToSunday(dto.getEventDate());
        String actor = dto.getActor() != null && !dto.getActor().isBlank()
                ? dto.getActor()
                : "System";

        AttendanceEvent event = attendanceEventRepository
                .findByEmployeeIdAndEventDate(employee.getId(), dto.getEventDate())
                .orElse(null);

        if (event == null) {
            event = AttendanceEvent.builder()
                    .employee(employee)
                    .eventDate(dto.getEventDate())
                    .weekStart(weekStart)
                    .kind(kind)
                    .details(dto.getDetails())
                    .source(dto.getSource() != null ? dto.getSource() : "Employee")
                    .createdBy(actor)
                    .createdAt(LocalDateTime.now())
                    .build();
        } else {
            event.setKind(kind);
            event.setDetails(dto.getDetails());
            event.setWeekStart(weekStart);
            event.setSource(dto.getSource() != null ? dto.getSource() : event.getSource());
            event.setUpdatedBy(actor);
            event.setUpdatedAt(LocalDateTime.now());
        }

        return map(attendanceEventRepository.save(event));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "attendance", key = "'employee:' + #employeeId")
    public List<AttendanceEventResponseDto> getByEmployee(Long employeeId) {
        return attendanceEventRepository.findByEmployeeIdOrderByEventDateDesc(employeeId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "attendance", key = "'employee-week:' + #employeeId + ':' + #weekStart")
    public List<AttendanceEventResponseDto> getByEmployeeAndWeek(Long employeeId, LocalDate weekStart) {
        LocalDate normalizedWeekStart = normalizeToSunday(weekStart);

        return attendanceEventRepository
                .findByEmployeeIdAndWeekStartOrderByEventDateAsc(employeeId, normalizedWeekStart)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "attendance", key = "'week:' + #weekStart")
    public List<AttendanceEventResponseDto> getByWeek(LocalDate weekStart) {
        LocalDate normalizedWeekStart = normalizeToSunday(weekStart);

        return attendanceEventRepository.findByWeekStartOrderByEventDateAsc(normalizedWeekStart)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"attendance", "timesheetWeeks", "timesheetIssues"}, allEntries = true)
    public void delete(Long attendanceEventId) {
        AttendanceEvent event = attendanceEventRepository.findById(attendanceEventId)
                .orElseThrow(() -> new EntityNotFoundException("Attendance event not found"));

        attendanceEventRepository.delete(event);
    }

    public boolean isExcusedMissingDay(Long employeeId, LocalDate eventDate) {
        return attendanceEventRepository.findByEmployeeIdAndEventDate(employeeId, eventDate)
                .map(event ->
                        event.getKind() == AttendanceEventKind.SICK_DAY
                                || event.getKind() == AttendanceEventKind.MISSED_DAY
                                || event.getKind() == AttendanceEventKind.VACATION
                )
                .orElse(false);
    }

    private AttendanceEventKind parseKind(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Attendance event kind is required");
        }

        String normalized = value.trim()
                .toUpperCase()
                .replace(" ", "_")
                .replace("-", "_");

        try {
            return AttendanceEventKind.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid attendance event kind: " + value);
        }
    }

    private LocalDate normalizeToSunday(LocalDate date) {
        while (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
            date = date.minusDays(1);
        }
        return date;
    }

    private AttendanceEventResponseDto map(AttendanceEvent event) {
        return AttendanceEventResponseDto.builder()
                .id(event.getId())
                .employeeId(event.getEmployee().getId())
                .employeeName(event.getEmployee().getName())
                .eventDate(event.getEventDate())
                .weekStart(event.getWeekStart())
                .kind(event.getKind().name())
                .details(event.getDetails())
                .source(event.getSource())
                .createdBy(event.getCreatedBy())
                .createdAt(event.getCreatedAt())
                .updatedBy(event.getUpdatedBy())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
