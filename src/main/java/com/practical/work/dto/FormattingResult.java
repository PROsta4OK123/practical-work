package com.practical.work.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormattingResult {
    
    private String formattedText;
    private String formattingType;
    private String fontStyle;
    private Integer fontSize;
    private String alignment;
} 