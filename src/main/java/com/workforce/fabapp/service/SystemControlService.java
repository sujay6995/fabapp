package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.*;
import com.workforce.fabapp.entity.*;
import com.workforce.fabapp.enums.HolidayScope;
import com.workforce.fabapp.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemControlService {

    private final HolidayRepository holidayRepository;
    private final BackupRecordRepository backupRecordRepository;

    private final TimesheetEntryRepository timesheetEntryRepository;
    private final TimesheetWeekRepository timesheetWeekRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ApprovalActionRepository approvalActionRepository;
    private final AuditLogRepository auditLogRepository;
    private final JobRequestRepository jobRequestRepository;
    private final WorkerTaskRepository workerTaskRepository;
    private final AttendanceEventRepository attendanceEventRepository;
    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    public List<HolidayResponseDto> getHolidays() {
        return holidayRepository.findAll()
                .stream()
                .map(this::mapHoliday)
                .toList();
    }

    @Transactional
    public HolidayResponseDto createHoliday(HolidayRequestDto dto) {
        Holiday holiday = Holiday.builder()
                .name(dto.getName())
                .date(dto.getDate())
                .observedDate(dto.getObservedDate())
                .scope(parseScope(dto.getScope()))
                .paidHours(dto.getPaidHours() != null ? dto.getPaidHours() : BigDecimal.valueOf(8))
                .active(dto.getActive())
                .build();

        return mapHoliday(holidayRepository.save(holiday));
    }

    @Transactional
    public HolidayResponseDto updateHoliday(Long id, HolidayRequestDto dto) {
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Holiday not found"));

        holiday.setName(dto.getName());
        holiday.setDate(dto.getDate());
        holiday.setObservedDate(dto.getObservedDate());
        holiday.setScope(parseScope(dto.getScope()));
        holiday.setPaidHours(dto.getPaidHours() != null ? dto.getPaidHours() : BigDecimal.valueOf(8));
        holiday.setActive(dto.getActive());

        return mapHoliday(holidayRepository.save(holiday));
    }

    @Transactional(readOnly = true)
    public List<BackupRecordDto> getBackupRecords() {
        return backupRecordRepository.findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapBackup)
                .toList();
    }

    @Transactional
    public Map<String, Object> generateBackup(String actor) {
        Map<String, Object> backup = new LinkedHashMap<>();

        backup.put("generatedAt", LocalDateTime.now().toString());
        backup.put("generatedBy", actor);

        backup.put("holidays", holidayRepository.findAll());
        backup.put("jobs", jobRepository.findAll());
        backup.put("timesheetWeeks", timesheetWeekRepository.findAll());
        backup.put("timesheetEntries", timesheetEntryRepository.findAll());
        backup.put("leaveRequests", leaveRequestRepository.findAll());
        backup.put("approvalActions", approvalActionRepository.findAll());
        backup.put("auditLog", auditLogRepository.findAll());
        backup.put("jobRequests", jobRequestRepository.findAll());
        backup.put("workerTasks", workerTaskRepository.findAll());
        backup.put("attendanceEvents", attendanceEventRepository.findAll());

        long count =
                holidayRepository.count()
                        + jobRepository.count()
                        + timesheetWeekRepository.count()
                        + timesheetEntryRepository.count()
                        + leaveRequestRepository.count()
                        + approvalActionRepository.count()
                        + auditLogRepository.count()
                        + jobRequestRepository.count()
                        + workerTaskRepository.count()
                        + attendanceEventRepository.count();

        String filename = "system-backup-" + System.currentTimeMillis() + ".json";

        backupRecordRepository.save(BackupRecord.builder()
                .filename(filename)
                .createdBy(actor)
                .createdAt(LocalDateTime.now())
                .recordCount(count)
                .note("Generated from System Control")
                .build());

        auditLogRepository.save(AuditLog.builder()
                .actor(actor)
                .item("System backup generated")
                .at(LocalDateTime.now())
                .build());

        return backup;
    }

    @Transactional
    @CacheEvict(value = {"timesheetWeeks", "timesheetIssues", "attendance", "overtimeAllocations", "doubleTimeAllocations"}, allEntries = true)
    public void resetOperationalData(SystemResetRequestDto dto) {
        if (!"RESET OPERATIONAL DATA".equalsIgnoreCase(dto.getConfirmation().trim())) {
            throw new IllegalArgumentException("Invalid reset confirmation.");
        }

        timesheetEntryRepository.deleteAll();
        timesheetWeekRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        approvalActionRepository.deleteAll();
        jobRequestRepository.deleteAll();
        workerTaskRepository.deleteAll();
        attendanceEventRepository.deleteAll();

        /*
         * Keep jobs but archive/clear operational flags if needed.
         * If you want reset to remove jobs too, uncomment:
         *
         * jobRepository.deleteAll();
         */

        auditLogRepository.save(AuditLog.builder()
                .actor(dto.getActor())
                .item("Operational data reset")
                .at(LocalDateTime.now())
                .build());
    }

    private HolidayScope parseScope(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Holiday scope is required.");
        }

        String normalized = value.trim()
                .toUpperCase()
                .replace(" ", "_")
                .replace("-", "_");

        if ("ONTARIO_ONLY".equals(normalized)) return HolidayScope.ONTARIO;
        if ("FEDERAL_ONLY".equals(normalized)) return HolidayScope.FEDERAL;

        try {
            return HolidayScope.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid holiday scope: " + value);
        }
    }

    private HolidayResponseDto mapHoliday(Holiday holiday) {
        return HolidayResponseDto.builder()
                .id(holiday.getId())
                .name(holiday.getName())
                .date(holiday.getDate())
                .observedDate(holiday.getObservedDate())
                .scope(holiday.getScope().name())
                .paidHours(holiday.getPaidHours())
                .active(holiday.getActive())
                .build();
    }

    private BackupRecordDto mapBackup(BackupRecord backup) {
        return BackupRecordDto.builder()
                .id(backup.getId())
                .filename(backup.getFilename())
                .createdBy(backup.getCreatedBy())
                .createdAt(backup.getCreatedAt())
                .recordCount(backup.getRecordCount())
                .note(backup.getNote())
                .build();
    }
}
