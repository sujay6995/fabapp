package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.OvertimeAllocationRequestDto;
import com.workforce.fabapp.dto.OvertimeAllocationResponseDto;
import com.workforce.fabapp.entity.Job;
import com.workforce.fabapp.entity.OvertimeAllocation;
import com.workforce.fabapp.entity.TimesheetEntry;
import com.workforce.fabapp.entity.TimesheetWeek;
import com.workforce.fabapp.enums.TimesheetStatus;
import com.workforce.fabapp.repository.JobRepository;
import com.workforce.fabapp.repository.OvertimeAllocationRepository;
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
public class OvertimeAllocationService {

    private final OvertimeAllocationRepository overtimeAllocationRepository;
    private final TimesheetWeekRepository timesheetWeekRepository;
    private final TimesheetEntryRepository timesheetEntryRepository;
    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "overtimeAllocations", key = "#weekId")
    public List<OvertimeAllocationResponseDto> getByWeek(Long weekId) {
        return overtimeAllocationRepository.findByTimesheetWeekIdWithDetailsOrderBySortOrderAscIdAsc(weekId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"overtimeAllocations", "timesheetWeeks"}, allEntries = true)
    public List<OvertimeAllocationResponseDto> replace(
            Long weekId,
            List<OvertimeAllocationRequestDto> rows,
            String actor
    ) {
        TimesheetWeek week = timesheetWeekRepository.findById(weekId)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet week not found"));

        if (Boolean.TRUE.equals(week.getPayrollLocked()) || week.getStatus() == TimesheetStatus.PAYROLL_LOCKED) {
            throw new IllegalStateException("Week is payroll locked.");
        }

        overtimeAllocationRepository.deleteByTimesheetWeekId(weekId);
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < rows.size(); i++) {
            OvertimeAllocationRequestDto row = rows.get(i);
            BigDecimal hours = row.getHours() == null ? BigDecimal.ZERO : row.getHours();
            if (hours.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Job job = row.getJobId() == null
                    ? null
                    : jobRepository.findById(row.getJobId())
                    .orElseThrow(() -> new EntityNotFoundException("Job not found"));

            overtimeAllocationRepository.save(OvertimeAllocation.builder()
                    .timesheetWeek(week)
                    .job(job)
                    .hours(hours.setScale(2, RoundingMode.HALF_UP))
                    .note(row.getNote())
                    .sortOrder(i)
                    .updatedBy(actor)
                    .updatedAt(now)
                    .build());
        }

        return getByWeek(weekId);
    }

    @Transactional
    @CacheEvict(value = {"overtimeAllocations", "timesheetWeeks"}, allEntries = true)
    public List<OvertimeAllocationResponseDto> ensureDefaultAllocations(Long weekId, String actor) {
        List<OvertimeAllocation> existing = overtimeAllocationRepository
                .findByTimesheetWeekIdWithDetailsOrderBySortOrderAscIdAsc(weekId);

        if (!existing.isEmpty()) {
            return existing.stream().map(this::map).toList();
        }

        TimesheetWeek week = timesheetWeekRepository.findById(weekId)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet week not found"));

        List<TimesheetEntry> entries = timesheetEntryRepository.findByTimesheetWeekIdWithDetails(weekId).stream()
                .filter(entry -> entry.getLeaveType() == null)
                .filter(entry -> entry.getHours() != null && entry.getHours().compareTo(BigDecimal.ZERO) > 0)
                .filter(entry -> entry.getWorkType() == null
                        || Boolean.TRUE.equals(entry.getWorkType().getCountsTowardOt()))
                .sorted(java.util.Comparator
                        .comparing(TimesheetEntry::getWorkDate)
                        .thenComparing(TimesheetEntry::getId))
                .toList();

        BigDecimal eligible = entries.stream()
                .map(TimesheetEntry::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingOt = eligible.subtract(BigDecimal.valueOf(44)).max(BigDecimal.ZERO);

        if (remainingOt.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        int sortOrder = 0;

        for (int i = entries.size() - 1; i >= 0 && remainingOt.compareTo(BigDecimal.ZERO) > 0; i--) {
            TimesheetEntry entry = entries.get(i);
            BigDecimal assigned = entry.getHours().min(remainingOt).setScale(2, RoundingMode.HALF_UP);
            if (assigned.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            overtimeAllocationRepository.save(OvertimeAllocation.builder()
                    .timesheetWeek(week)
                    .job(entry.getJob())
                    .hours(assigned)
                    .note("Default OT from " + entry.getWorkDate())
                    .sortOrder(sortOrder++)
                    .updatedBy(actor)
                    .updatedAt(now)
                    .build());

            remainingOt = remainingOt.subtract(assigned).max(BigDecimal.ZERO);
        }

        return getByWeek(weekId);
    }

    private OvertimeAllocationResponseDto map(OvertimeAllocation row) {
        return OvertimeAllocationResponseDto.builder()
                .id(row.getId())
                .timesheetWeekId(row.getTimesheetWeek().getId())
                .jobId(row.getJob() != null ? row.getJob().getId() : null)
                .jobCode(row.getJob() != null ? row.getJob().getCode() : null)
                .jobName(row.getJob() != null ? row.getJob().getName() : null)
                .hours(row.getHours())
                .note(row.getNote())
                .sortOrder(row.getSortOrder())
                .updatedBy(row.getUpdatedBy())
                .updatedAt(row.getUpdatedAt())
                .build();
    }
}
