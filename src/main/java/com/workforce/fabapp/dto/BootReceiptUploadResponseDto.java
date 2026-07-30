package com.workforce.fabapp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BootReceiptUploadResponseDto {

    private String storageKey;
    private String downloadUrl;
    private String fileName;
    private String contentType;
    private long size;
}
