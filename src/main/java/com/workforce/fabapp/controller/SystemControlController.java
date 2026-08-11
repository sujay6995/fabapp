package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.*;
import com.workforce.fabapp.service.SystemControlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemControlController {

    private final SystemControlService systemControlService;
    private final ObjectMapper objectMapper;

    @GetMapping("/holidays")
    public List<HolidayResponseDto> getHolidays() {
        return systemControlService.getHolidays();
    }

    @PostMapping("/holidays")
    public HolidayResponseDto createHoliday(@Valid @RequestBody HolidayRequestDto dto) {
        return systemControlService.createHoliday(dto);
    }

    @PutMapping("/holidays/{id}")
    public HolidayResponseDto updateHoliday(
            @PathVariable Long id,
            @Valid @RequestBody HolidayRequestDto dto
    ) {
        return systemControlService.updateHoliday(id, dto);
    }

    @GetMapping("/backups")
    public List<BackupRecordDto> getBackupRecords() {
        return systemControlService.getBackupRecords();
    }

    @GetMapping("/backup/export")
    public ResponseEntity<byte[]> exportBackup(@RequestParam(defaultValue = "System") String actor) throws Exception {
        Map<String, Object> backup = systemControlService.generateBackup(actor);

        byte[] json = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(backup)
                .getBytes(StandardCharsets.UTF_8);

        String filename = "system-backup-" + System.currentTimeMillis() + ".json";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @PostMapping("/reset-operational-data")
    public void resetOperationalData(@Valid @RequestBody SystemResetRequestDto dto) {
        systemControlService.resetOperationalData(dto);
    }
}
