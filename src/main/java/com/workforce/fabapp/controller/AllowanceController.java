package com.workforce.fabapp.controller;

import com.workforce.fabapp.dto.BootAllowanceRequestDto;
import com.workforce.fabapp.dto.BootAllowanceResponseDto;
import com.workforce.fabapp.dto.BootAllowanceReviewRequestDto;
import com.workforce.fabapp.dto.BootReceiptUploadResponseDto;
import com.workforce.fabapp.entity.BootReceipt;
import com.workforce.fabapp.service.BootAllowanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/allowances")
@RequiredArgsConstructor
public class AllowanceController {

    private final BootAllowanceService bootAllowanceService;

    @PostMapping(value = "/boots/receipt-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BootReceiptUploadResponseDto uploadBootReceipt(@RequestPart("receipt") MultipartFile receipt) {
        return bootAllowanceService.uploadReceipt(receipt);
    }

    @PostMapping("/boots")
    public BootAllowanceResponseDto createBootAllowance(@Valid @RequestBody BootAllowanceRequestDto dto) {
        return bootAllowanceService.create(dto);
    }

    @GetMapping("/boots")
    public List<BootAllowanceResponseDto> getBootAllowances(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String status
    ) {
        if (employeeId != null) {
            return bootAllowanceService.getByEmployee(employeeId);
        }
        if ("pending".equalsIgnoreCase(status) || "PENDING_PAYROLL".equalsIgnoreCase(status)) {
            return bootAllowanceService.getPending();
        }
        return bootAllowanceService.getAll();
    }

    @PostMapping("/boots/{requestId}/approve")
    public BootAllowanceResponseDto approveBootAllowance(
            @PathVariable Long requestId,
            @RequestBody(required = false) BootAllowanceReviewRequestDto dto
    ) {
        return bootAllowanceService.approve(requestId, dto != null ? dto.getActor() : "System");
    }

    @PostMapping("/boots/{requestId}/reject")
    public BootAllowanceResponseDto rejectBootAllowance(
            @PathVariable Long requestId,
            @RequestBody(required = false) BootAllowanceReviewRequestDto dto
    ) {
        return bootAllowanceService.reject(requestId, dto != null ? dto.getActor() : "System");
    }

    @GetMapping("/boots/receipts/{storageKey}")
    public ResponseEntity<byte[]> getBootReceipt(@PathVariable String storageKey) {
        BootReceipt receipt = bootAllowanceService.getReceipt(storageKey);
        MediaType mediaType = MediaType.parseMediaType(receipt.getContentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(receipt.getSize())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + receipt.getFileName() + "\"")
                .body(receipt.getData());
    }
}
