package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.AttendanceEventRequestDto;
import com.workforce.fabapp.dto.AttendanceEventResponseDto;
import com.workforce.fabapp.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    public AttendanceEventResponseDto upsert(@Valid @RequestBody AttendanceEventRequestDto dto) {
        return attendanceService.upsert(dto);
    }

    @GetMapping("/employee/{employeeId}")
    public List<AttendanceEventResponseDto> getByEmployee(@PathVariable Long employeeId) {
        return attendanceService.getByEmployee(employeeId);
    }

    @GetMapping("/employee/{employeeId}/week")
    public List<AttendanceEventResponseDto> getByEmployeeAndWeek(
            @PathVariable Long employeeId,
            @RequestParam LocalDate weekStart
    ) {
        return attendanceService.getByEmployeeAndWeek(employeeId, weekStart);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @GetMapping("/week")
    public List<AttendanceEventResponseDto> getByWeek(@RequestParam LocalDate weekStart) {
        return attendanceService.getByWeek(weekStart);
    }

    @DeleteMapping("/{attendanceEventId}")
    public void delete(@PathVariable Long attendanceEventId) {
        attendanceService.delete(attendanceEventId);
    }
}
