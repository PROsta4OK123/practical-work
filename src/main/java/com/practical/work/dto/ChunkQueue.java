package com.practical.work.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChunkQueue {
    
    private int queueId;
    private List<TextChunk> chunks;
    private String fileId;
} 