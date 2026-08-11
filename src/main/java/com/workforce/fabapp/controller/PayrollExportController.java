package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.PayrollExportResponseDto;
import com.workforce.fabapp.service.PayrollExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PayrollExportController {

    private final PayrollExportService payrollExportService;

    @GetMapping("/review")
    public PayrollExportResponseDto review(@RequestParam LocalDate weekStart) {
        return payrollExportService.getPayrollReview(weekStart);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam LocalDate weekStart) {
        byte[] csv = payrollExportService.exportCsv(weekStart);

        String filename = "payroll-export-" + weekStart + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
