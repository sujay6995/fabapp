package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class BootAllowanceResponseDto {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private LocalDate purchaseDate;
    private BigDecimal amount;
    private String note;
    private String receiptFileName;
    private String receiptStorageKey;
    private String receiptUrl;
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private String reviewedBy;
    private Integer version;
}
