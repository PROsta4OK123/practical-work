package com.practical.work.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TextChunk {
    
    private int index;
    private String text;
    private String paragraphId; // Для связи с оригинальным абзацем
} 