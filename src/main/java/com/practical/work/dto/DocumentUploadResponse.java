package com.practical.work.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUploadResponse {
    
    private boolean success;
    private String fileId;
    private String originalName;
    private Long size;
    private String message;
    private Integer queuePosition;
} 