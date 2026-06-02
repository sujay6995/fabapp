package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.ScheduleDayDto;
import com.workforce.fabapp.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/employee/{employeeId}/month")
    public List<ScheduleDayDto> getMonthlySchedule(
            @PathVariable Long employeeId,
            @RequestParam String month
    ) {
        return scheduleService.getMonthlySchedule(employeeId, month);
    }
}
