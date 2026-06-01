package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.DoubleTimeAllocationRequestDto;
import com.workforce.fabapp.dto.DoubleTimeAllocationResponseDto;
import com.workforce.fabapp.entity.*;
import com.workforce.fabapp.enums.DoubleTimeStatus;
import com.workforce.fabapp.enums.TimesheetStatus;
import com.workforce.fabapp.repository.DoubleTimeAllocationRepository;
import com.workforce.fabapp.repository.JobRepository;
import com.workforce.fabapp.repository.TimesheetEntryRepository;
import com.workforce.fabapp.repository.TimesheetWeekRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoubleTimeAllocationService {

    private final DoubleTimeAllocationRepository doubleTimeAllocationRepository;
    private final TimesheetWeekRepository timesheetWeekRepository;
    private final TimesheetEntryRepository timesheetEntryRepository;
    private final JobRepository jobRepository;

    @Transactional
    @CacheEvict(value = {"doubleTimeAllocations", "timesheetWeeks"}, allEntries = true)
    public DoubleTimeAllocationResponseDto create(DoubleTimeAllocationRequestDto dto) {
        TimesheetWeek week = timesheetWeekRepository.findById(dto.getTimesheetWeekId())
                .orElseThrow(() -> new EntityNotFoundException("Timesheet week not found"));

        if (Boolean.TRUE.equals(week.getPayrollLocked()) || week.getStatus() == TimesheetStatus.PAYROLL_LOCKED) {
            throw new IllegalStateException("Week is payroll locked.");
        }

        TimesheetEntry entry = null;
        if (dto.getTimesheetEntryId() != null) {
            entry = timesheetEntryRepository.findById(dto.getTimesheetEntryId())
                    .orElseThrow(() -> new EntityNotFoundException("Timesheet entry not found"));

            if (!entry.getTimesheetWeek().getId().equals(week.getId())) {
                throw new IllegalStateException("Timesheet entry does not belong to the selected week.");
            }
        }

        Job job = null;
        if (dto.getJobId() != null) {
            job = jobRepository.findById(dto.getJobId())
                    .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        } else if (entry != null && entry.getJob() != null) {
            job = entry.getJob();
        }

        if (job == null) {
            throw new IllegalStateException("Double-time allocation requires a job.");
        }

        BigDecimal weekWorkedHours = timesheetEntryRepository.findByTimesheetWeekId(week.getId())
                .stream()
                .filter(e -> e.getLeaveType() == null)
                .map(e -> e.getHours() != null ? e.getHours() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal existingDtHours = doubleTimeAllocationRepository
                .findByTimesheetWeekIdAndStatusWithDetails(week.getId(), DoubleTimeStatus.ACTIVE)
                .stream()
                .map(DoubleTimeAllocation::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (existingDtHours.add(dto.getHours()).compareTo(weekWorkedHours) > 0) {
            throw new IllegalStateException("Double-time hours cannot exceed worked hours for the week.");
        }

        DoubleTimeAllocation allocation = DoubleTimeAllocation.builder()
                .timesheetWeek(week)
                .timesheetEntry(entry)
                .job(job)
                .hours(dto.getHours())
                .note(dto.getNote())
                .status(DoubleTimeStatus.ACTIVE)
                .createdBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "System")
                .createdAt(LocalDateTime.now())
                .build();

        return map(doubleTimeAllocationRepository.save(allocation));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "doubleTimeAllocations", key = "#weekId")
    public List<DoubleTimeAllocationResponseDto> getByWeek(Long weekId) {
        return doubleTimeAllocationRepository
                .findByTimesheetWeekIdAndStatusWithDetails(weekId, DoubleTimeStatus.ACTIVE)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"doubleTimeAllocations", "timesheetWeeks"}, allEntries = true)
    public DoubleTimeAllocationResponseDto remove(Long allocationId, String removedBy) {
        DoubleTimeAllocation allocation = doubleTimeAllocationRepository.findByIdWithDetails(allocationId)
                .orElseThrow(() -> new EntityNotFoundException("Double-time allocation not found"));

        if (Boolean.TRUE.equals(allocation.getTimesheetWeek().getPayrollLocked())
                || allocation.getTimesheetWeek().getStatus() == TimesheetStatus.PAYROLL_LOCKED) {
            throw new IllegalStateException("Week is payroll locked.");
        }

        allocation.setStatus(DoubleTimeStatus.REMOVED);
        allocation.setRemovedBy(removedBy != null ? removedBy : "System");
        allocation.setRemovedAt(LocalDateTime.now());

        return map(doubleTimeAllocationRepository.save(allocation));
    }

    private DoubleTimeAllocationResponseDto map(DoubleTimeAllocation allocation) {
        TimesheetWeek week = allocation.getTimesheetWeek();
        Job job = allocation.getJob();

        return DoubleTimeAllocationResponseDto.builder()
                .id(allocation.getId())
                .timesheetWeekId(week.getId())
                .timesheetEntryId(allocation.getTimesheetEntry() != null ? allocation.getTimesheetEntry().getId() : null)
                .employeeId(week.getEmployee().getId())
                .employeeName(week.getEmployee().getName())
                .jobId(job != null ? job.getId() : null)
                .jobCode(job != null ? job.getCode() : null)
                .jobName(job != null ? job.getName() : null)
                .hours(scale(allocation.getHours()))
                .note(allocation.getNote())
                .status(allocation.getStatus().name())
                .createdBy(allocation.getCreatedBy())
                .createdAt(allocation.getCreatedAt())
                .removedBy(allocation.getRemovedBy())
                .removedAt(allocation.getRemovedAt())
                .build();
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
