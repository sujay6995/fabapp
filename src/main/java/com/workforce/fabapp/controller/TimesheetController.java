package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.OvertimeAllocationRequestDto;
import com.workforce.fabapp.dto.OvertimeAllocationResponseDto;
import com.workforce.fabapp.dto.TimesheetEntryRequestDto;
import com.workforce.fabapp.dto.TimesheetEntryResponseDto;
import com.workforce.fabapp.dto.TimesheetIssueDto;
import com.workforce.fabapp.dto.TimesheetWeekResponseDto;
import com.workforce.fabapp.service.OvertimeAllocationService;
import com.workforce.fabapp.service.TimesheetService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetService timesheetService;
    private final OvertimeAllocationService overtimeAllocationService;

    @GetMapping("/week")
    public TimesheetWeekResponseDto getWeek(
            @RequestParam Long employeeId,
            @RequestParam LocalDate weekStart
    ) {
        return timesheetService.getOrCreateWeek(employeeId, weekStart);
    }

    @PostMapping("/entries")
    public TimesheetEntryResponseDto addEntry(
            @Valid @RequestBody TimesheetEntryRequestDto dto,
            @RequestParam(defaultValue = "System") String actor
    ) {
        return timesheetService.addEntry(dto, actor);
    }

    @PutMapping("/entries/{entryId}")
    public TimesheetEntryResponseDto updateEntry(
            @PathVariable Long entryId,
            @Valid @RequestBody TimesheetEntryRequestDto dto,
            @RequestParam(defaultValue = "System") String actor
    ) {
        return timesheetService.updateEntry(entryId, dto, actor);
    }

    @DeleteMapping("/entries/{entryId}")
    public void deleteEntry(
            @PathVariable Long entryId,
            @RequestParam(defaultValue = "System") String actor
    ) {
        timesheetService.deleteEntry(entryId, actor);
    }

    @PostMapping("/{weekId}/submit")
    public TimesheetWeekResponseDto submitWeek(@PathVariable Long weekId) {
        return timesheetService.submitWeek(weekId);
    }

    @GetMapping("/{weekId}/issues")
    public List<TimesheetIssueDto> getIssues(@PathVariable Long weekId) {
        return timesheetService.getWeekIssues(weekId);
    }

    @GetMapping("/{weekId}/ot-allocations")
    public List<OvertimeAllocationResponseDto> getOvertimeAllocations(@PathVariable Long weekId) {
        return overtimeAllocationService.getByWeek(weekId);
    }

    @PutMapping("/{weekId}/ot-allocations")
    public List<OvertimeAllocationResponseDto> replaceOvertimeAllocations(
            @PathVariable Long weekId,
            @Valid @RequestBody List<OvertimeAllocationRequestDto> rows,
            @RequestParam(defaultValue = "System") String actor
    ) {
        return overtimeAllocationService.replace(weekId, rows, actor);
    }

    @GetMapping("/supervisor/{supervisorId}/week")
    public List<TimesheetWeekResponseDto> getSupervisorWeeks(
            @PathVariable Long supervisorId,
            @RequestParam LocalDate weekStart
    ) {
        return timesheetService.getSupervisorWeeks(supervisorId, weekStart);
    }
}
