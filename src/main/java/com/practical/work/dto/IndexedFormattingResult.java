package com.practical.work.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexedFormattingResult {
    
    private int index;
    private String paragraphId;
    private FormattingResult formattingResult;
    private boolean success;
    private String errorMessage;
} 