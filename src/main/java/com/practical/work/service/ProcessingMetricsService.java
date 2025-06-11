package com.practical.work.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class ProcessingMetricsService {

    private final ConcurrentHashMap<String, ProcessingMetrics> activeProcessings = new ConcurrentHashMap<>();
    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);

    public void startProcessing(String fileId, int totalChunks) {
        ProcessingMetrics metrics = ProcessingMetrics.builder()
            .fileId(fileId)
            .totalChunks(totalChunks)
            .startTime(LocalDateTime.now())
            .processedChunks(new AtomicInteger(0))
            .build();
            
        activeProcessings.put(fileId, metrics);
        log.info("Начат мониторинг обработки файла {} с {} блоками", fileId, totalChunks);
    }

    public void chunkProcessed(String fileId) {
        ProcessingMetrics metrics = activeProcessings.get(fileId);
        if (metrics != null) {
            int processed = metrics.getProcessedChunks().incrementAndGet();
            log.debug("Обработан блок {}/{} для файла {}", 
                processed, metrics.getTotalChunks(), fileId);
        }
    }

    public void finishProcessing(String fileId) {
        ProcessingMetrics metrics = activeProcessings.remove(fileId);
        if (metrics != null) {
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(metrics.getStartTime(), endTime);
            
            totalProcessed.incrementAndGet();
            totalProcessingTime.addAndGet(duration.toMillis());
            
            log.info("Завершена обработка файла {}. Время: {}мс, Блоков: {}, Скорость: {:.2f} блоков/сек",
                fileId, 
                duration.toMillis(),
                metrics.getTotalChunks(),
                metrics.getTotalChunks() / (duration.toMillis() / 1000.0));
        }
    }

    public ProcessingStatus getProcessingStatus(String fileId) {
        ProcessingMetrics metrics = activeProcessings.get(fileId);
        if (metrics == null) {
            return null;
        }

        int processed = metrics.getProcessedChunks().get();
        double progress = (double) processed / metrics.getTotalChunks() * 100;
        
        return ProcessingStatus.builder()
            .fileId(fileId)
            .totalChunks(metrics.getTotalChunks())
            .processedChunks(processed)
            .progress(progress)
            .startTime(metrics.getStartTime())
            .build();
    }

    public GlobalMetrics getGlobalMetrics() {
        double avgProcessingTime = totalProcessed.get() > 0 ? 
            (double) totalProcessingTime.get() / totalProcessed.get() : 0;

        // Оценка нагрузки на систему
        int activeFiles = activeProcessings.size();
        String systemLoad = calculateSystemLoad(activeFiles);
            
        return GlobalMetrics.builder()
            .totalProcessedDocuments(totalProcessed.get())
            .activeProcessings(activeFiles)
            .averageProcessingTimeMs(avgProcessingTime)
            .systemLoad(systemLoad)
            .maxConcurrentFiles(3) // До 3 файлов одновременно (зависит от размера)
            .build();
    }

    private String calculateSystemLoad(int activeFiles) {
        if (activeFiles == 0) {
            return "LOW";
        } else if (activeFiles == 1) {
            return "MEDIUM";
        } else if (activeFiles >= 2) {
            return "HIGH";
        } else {
            return "CRITICAL";
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class ProcessingMetrics {
        private String fileId;
        private int totalChunks;
        private LocalDateTime startTime;
        private AtomicInteger processedChunks;
    }

    @lombok.Data
    @lombok.Builder
    public static class ProcessingStatus {
        private String fileId;
        private int totalChunks;
        private int processedChunks;
        private double progress;
        private LocalDateTime startTime;
    }

    @lombok.Data
    @lombok.Builder
    public static class GlobalMetrics {
        private int totalProcessedDocuments;
        private int activeProcessings;
        private double averageProcessingTimeMs;
        private String systemLoad;
        private int maxConcurrentFiles;
    }
} 