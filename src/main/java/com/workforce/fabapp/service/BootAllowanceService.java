package com.workforce.fabapp.service;

import com.workforce.fabapp.dto.BootAllowanceRequestDto;
import com.workforce.fabapp.dto.BootAllowanceResponseDto;
import com.workforce.fabapp.dto.BootReceiptUploadResponseDto;
import com.workforce.fabapp.entity.BootAllowanceRequest;
import com.workforce.fabapp.entity.BootReceipt;
import com.workforce.fabapp.entity.Employee;
import com.workforce.fabapp.enums.BootAllowanceStatus;
import com.workforce.fabapp.repository.BootAllowanceRequestRepository;
import com.workforce.fabapp.repository.BootReceiptRepository;
import com.workforce.fabapp.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BootAllowanceService {

    private static final long MAX_RECEIPT_BYTES = 10L * 1024L * 1024L;

    private final BootAllowanceRequestRepository bootAllowanceRequestRepository;
    private final BootReceiptRepository bootReceiptRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public BootReceiptUploadResponseDto uploadReceipt(MultipartFile receipt) {
        if (receipt == null || receipt.isEmpty()) {
            throw new IllegalArgumentException("Receipt photo is required.");
        }

        if (receipt.getSize() > MAX_RECEIPT_BYTES) {
            throw new IllegalArgumentException("Receipt photo must be 10 MB or smaller.");
        }

        String contentType = receipt.getContentType() == null ? "" : receipt.getContentType();
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Receipt must be an image.");
        }

        String storageKey = UUID.randomUUID().toString();
        String fileName = receipt.getOriginalFilename() == null || receipt.getOriginalFilename().isBlank()
                ? "receipt"
                : receipt.getOriginalFilename();

        try {
            bootReceiptRepository.save(BootReceipt.builder()
                    .storageKey(storageKey)
                    .fileName(fileName)
                    .contentType(contentType)
                    .size(receipt.getSize())
                    .data(receipt.getBytes())
                    .uploadedAt(LocalDateTime.now())
                    .build());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read receipt photo.");
        }

        return BootReceiptUploadResponseDto.builder()
                .storageKey(storageKey)
                .downloadUrl(receiptUrl(storageKey))
                .fileName(fileName)
                .contentType(contentType)
                .size(receipt.getSize())
                .build();
    }

    @Transactional
    public BootAllowanceResponseDto create(BootAllowanceRequestDto dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        BootReceipt receipt = bootReceiptRepository.findByStorageKey(dto.getReceiptStorageKey())
                .orElseThrow(() -> new EntityNotFoundException("Receipt upload not found"));

        BigDecimal amount = dto.getAmount() == null ? BigDecimal.ZERO : dto.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Boot allowance amount must be greater than zero.");
        }

        BootAllowanceRequest request = bootAllowanceRequestRepository.save(BootAllowanceRequest.builder()
                .employee(employee)
                .purchaseDate(dto.getPurchaseDate())
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .note(dto.getNote())
                .receiptFileName(dto.getReceiptFileName())
                .receiptStorageKey(dto.getReceiptStorageKey())
                .receipt(receipt)
                .status(BootAllowanceStatus.PENDING_PAYROLL)
                .submittedAt(LocalDateTime.now())
                .build());

        return map(request);
    }

    @Transactional(readOnly = true)
    public List<BootAllowanceResponseDto> getAll() {
        return bootAllowanceRequestRepository.findAllWithDetailsOrderBySubmittedAtDesc()
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BootAllowanceResponseDto> getByEmployee(Long employeeId) {
        return bootAllowanceRequestRepository.findByEmployeeIdWithDetailsOrderBySubmittedAtDesc(employeeId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BootAllowanceResponseDto> getPending() {
        return bootAllowanceRequestRepository
                .findByStatusWithDetailsOrderBySubmittedAtDesc(BootAllowanceStatus.PENDING_PAYROLL)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public BootAllowanceResponseDto approve(Long requestId, String actor) {
        return review(requestId, BootAllowanceStatus.APPROVED, actor);
    }

    @Transactional
    public BootAllowanceResponseDto reject(Long requestId, String actor) {
        return review(requestId, BootAllowanceStatus.REJECTED, actor);
    }

    @Transactional(readOnly = true)
    public BootReceipt getReceipt(String storageKey) {
        return bootReceiptRepository.findByStorageKey(storageKey)
                .orElseThrow(() -> new EntityNotFoundException("Receipt not found"));
    }

    private BootAllowanceResponseDto review(Long requestId, BootAllowanceStatus status, String actor) {
        BootAllowanceRequest request = bootAllowanceRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Boot allowance request not found"));

        if (request.getStatus() != BootAllowanceStatus.PENDING_PAYROLL) {
            throw new IllegalStateException("Only pending boot allowance requests can be reviewed.");
        }

        request.setStatus(status);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(actor == null || actor.isBlank() ? "System" : actor);

        return map(bootAllowanceRequestRepository.save(request));
    }

    private BootAllowanceResponseDto map(BootAllowanceRequest request) {
        return BootAllowanceResponseDto.builder()
                .id(request.getId())
                .employeeId(request.getEmployee().getId())
                .employeeName(request.getEmployee().getName())
                .purchaseDate(request.getPurchaseDate())
                .amount(request.getAmount())
                .note(request.getNote())
                .receiptFileName(request.getReceiptFileName())
                .receiptStorageKey(request.getReceiptStorageKey())
                .receiptUrl(receiptUrl(request.getReceiptStorageKey()))
                .status(request.getStatus().name())
                .submittedAt(request.getSubmittedAt())
                .reviewedAt(request.getReviewedAt())
                .reviewedBy(request.getReviewedBy())
                .version(1)
                .build();
    }

    private String receiptUrl(String storageKey) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/allowances/boots/receipts/")
                .path(storageKey)
                .toUriString();
    }
}
