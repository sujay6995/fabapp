package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BackupRecordDto {

    private Long id;
    private String filename;
    private String createdBy;
    private LocalDateTime createdAt;
    private Long recordCount;
    private String note;
}