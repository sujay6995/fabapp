package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.DoubleTimeAllocationRequestDto;
import com.workforce.fabapp.dto.DoubleTimeAllocationResponseDto;
import com.workforce.fabapp.service.DoubleTimeAllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/double-time")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
public class DoubleTimeAllocationController {

    private final DoubleTimeAllocationService doubleTimeAllocationService;

    @PostMapping
    public DoubleTimeAllocationResponseDto create(@Valid @RequestBody DoubleTimeAllocationRequestDto dto) {
        return doubleTimeAllocationService.create(dto);
    }

    @GetMapping("/week/{weekId}")
    public List<DoubleTimeAllocationResponseDto> getByWeek(@PathVariable Long weekId) {
        return doubleTimeAllocationService.getByWeek(weekId);
    }

    @DeleteMapping("/{allocationId}")
    public DoubleTimeAllocationResponseDto remove(
            @PathVariable Long allocationId,
            @RequestParam(required = false) String removedBy
    ) {
        return doubleTimeAllocationService.remove(allocationId, removedBy);
    }
}
