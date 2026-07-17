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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        LocalDateTime now = LocalDateTime.now();
        List<PreparedOvertimeAllocation> preparedRows = prepareRows(week, rows);

        overtimeAllocationRepository.deleteByTimesheetWeekId(weekId);

        for (PreparedOvertimeAllocation preparedRow : preparedRows) {
            overtimeAllocationRepository.save(OvertimeAllocation.builder()
                    .timesheetWeek(week)
                    .sourceEntry(preparedRow.sourceEntry())
                    .job(preparedRow.job())
                    .hours(preparedRow.hours())
                    .note(preparedRow.note())
                    .sortOrder(preparedRow.sortOrder())
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
                    .sourceEntry(entry)
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
                .sourceEntryId(row.getSourceEntry() != null ? row.getSourceEntry().getId() : null)
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

    private List<PreparedOvertimeAllocation> prepareRows(
            TimesheetWeek week,
            List<OvertimeAllocationRequestDto> rows
    ) {
        Map<Long, TimesheetEntry> sourceEntriesById = new HashMap<>();
        Map<Long, BigDecimal> allocatedBySourceEntryId = new HashMap<>();

        return java.util.stream.IntStream.range(0, rows.size())
                .mapToObj(index -> prepareRow(
                        week,
                        rows.get(index),
                        index,
                        sourceEntriesById,
                        allocatedBySourceEntryId
                ))
                .filter(row -> row != null)
                .toList();
    }

    private PreparedOvertimeAllocation prepareRow(
            TimesheetWeek week,
            OvertimeAllocationRequestDto row,
            int sortOrder,
            Map<Long, TimesheetEntry> sourceEntriesById,
            Map<Long, BigDecimal> allocatedBySourceEntryId
    ) {
        BigDecimal hours = row.getHours() == null ? BigDecimal.ZERO : row.getHours();
        if (hours.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        if (row.getSourceEntryId() == null) {
            throw new IllegalArgumentException("Source entry is required for overtime allocation.");
        }

        BigDecimal normalizedHours = hours.setScale(2, RoundingMode.HALF_UP);
        TimesheetEntry sourceEntry = sourceEntriesById.computeIfAbsent(row.getSourceEntryId(), sourceEntryId ->
                timesheetEntryRepository.findById(sourceEntryId)
                        .orElseThrow(() -> new EntityNotFoundException("Source timesheet entry not found"))
        );

        validateSourceEntry(week, sourceEntry);

        BigDecimal allocatedForSource = allocatedBySourceEntryId
                .getOrDefault(sourceEntry.getId(), BigDecimal.ZERO)
                .add(normalizedHours);
        BigDecimal sourceHours = sourceEntry.getHours() == null ? BigDecimal.ZERO : sourceEntry.getHours();

        if (allocatedForSource.compareTo(sourceHours) > 0) {
            throw new IllegalArgumentException(
                    "Allocated overtime cannot exceed submitted hours for source entry " + sourceEntry.getId()
            );
        }

        allocatedBySourceEntryId.put(sourceEntry.getId(), allocatedForSource);

        Job job = row.getJobId() == null
                ? sourceEntry.getJob()
                : jobRepository.findById(row.getJobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));

        return new PreparedOvertimeAllocation(
                sourceEntry,
                job,
                normalizedHours,
                row.getNote(),
                sortOrder
        );
    }

    private void validateSourceEntry(TimesheetWeek week, TimesheetEntry sourceEntry) {
        if (!sourceEntry.getTimesheetWeek().getId().equals(week.getId())) {
            throw new IllegalArgumentException("Source entry does not belong to the selected week.");
        }

        if (sourceEntry.getLeaveType() != null) {
            throw new IllegalArgumentException("Leave entries cannot be used as overtime source entries.");
        }

        if (sourceEntry.getHours() == null || sourceEntry.getHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Source entry must have submitted hours.");
        }
    }

    private record PreparedOvertimeAllocation(
            TimesheetEntry sourceEntry,
            Job job,
            BigDecimal hours,
            String note,
            Integer sortOrder
    ) {
    }
}
