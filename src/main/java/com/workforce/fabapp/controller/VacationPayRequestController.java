package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.CreateVacationPayRequestDto;
import com.workforce.fabapp.dto.VacationPayRequestResponseDto;
import com.workforce.fabapp.service.VacationPayRequestService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vacation-pay-requests")
@RequiredArgsConstructor
public class VacationPayRequestController {

    private final VacationPayRequestService vacationPayRequestService;

    @PostMapping
    public VacationPayRequestResponseDto create(@Valid @RequestBody CreateVacationPayRequestDto dto) {
        return vacationPayRequestService.create(dto);
    }

    @GetMapping("/employee/{employeeId}")
    public List<VacationPayRequestResponseDto> getByEmployee(@PathVariable Long employeeId) {
        return vacationPayRequestService.getByEmployee(employeeId);
    }

    @GetMapping
    public List<VacationPayRequestResponseDto> getByWeek(@RequestParam LocalDate weekStart) {
        return vacationPayRequestService.getOverlappingWeek(weekStart);
    }

    @PostMapping("/{requestId}/processed")
    public VacationPayRequestResponseDto processed(
            @PathVariable Long requestId,
            @RequestBody(required = false) VacationPayActionRequestDto dto
    ) {
        return vacationPayRequestService.processed(requestId, dto != null ? dto.getActor() : "System");
    }

    @PostMapping("/{requestId}/cancel")
    public VacationPayRequestResponseDto cancel(
            @PathVariable Long requestId,
            @RequestBody(required = false) VacationPayActionRequestDto dto
    ) {
        return vacationPayRequestService.cancel(requestId, dto != null ? dto.getActor() : "System");
    }

    @Data
    public static class VacationPayActionRequestDto {
        private String actor;
    }
}
