package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.CreateWorkerTaskDto;
import com.workforce.fabapp.dto.WorkerTaskResponseDto;
import com.workforce.fabapp.dto.WorkerTaskResponseRequestDto;
import com.workforce.fabapp.service.WorkerTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin
public class WorkerTaskController {

    private final WorkerTaskService workerTaskService;

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @PostMapping
    public WorkerTaskResponseDto create(@Valid @RequestBody CreateWorkerTaskDto dto) {
        return workerTaskService.create(dto);
    }

    @GetMapping("/employee/{employeeId}")
    public List<WorkerTaskResponseDto> getForEmployee(@PathVariable Long employeeId) {
        return workerTaskService.getForEmployee(employeeId);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    @GetMapping("/supervisor/{supervisorId}")
    public List<WorkerTaskResponseDto> getForSupervisor(@PathVariable Long supervisorId) {
        return workerTaskService.getForSupervisor(supervisorId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<WorkerTaskResponseDto> getAll() {
        return workerTaskService.getAll();
    }

    @PostMapping("/{taskId}/seen")
    public WorkerTaskResponseDto markSeen(@PathVariable Long taskId) {
        return workerTaskService.markSeen(taskId);
    }

    @PostMapping("/{taskId}/respond")
    public WorkerTaskResponseDto respond(
            @PathVariable Long taskId,
            @Valid @RequestBody WorkerTaskResponseRequestDto dto
    ) {
        return workerTaskService.respond(taskId, dto);
    }

    @PostMapping("/{taskId}/complete")
    public WorkerTaskResponseDto complete(@PathVariable Long taskId) {
        return workerTaskService.complete(taskId);
    }
}