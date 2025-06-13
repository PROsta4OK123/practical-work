package com.practical.work.service;

import com.practical.work.event.DocumentChunksCountUpdatedEvent;
import com.practical.work.model.ProcessedDocument;
import com.practical.work.repository.ProcessedDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class ProcessingMetricsService {

    private final ConcurrentHashMap<String, ProcessingMetrics> activeProcessings = new ConcurrentHashMap<>();
    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    
    @Autowired
    private ProcessedDocumentRepository documentRepository;
    
    /**
     * Обработчик события обновления количества чанков
     * Создает начальные метрики для документа до начала обработки
     */
    @EventListener
    public void handleChunksCountUpdated(DocumentChunksCountUpdatedEvent event) {
        String fileId = event.getFileId();
        int chunksCount = event.getChunksCount();
        
        log.info("Получено событие обновления количества чанков для документа {}: {} чанков", 
            fileId, chunksCount);
        
        // Если документ еще не в обработке, создаем предварительные метрики
        if (!activeProcessings.containsKey(fileId)) {
            ProcessingMetrics metrics = ProcessingMetrics.builder()
                .fileId(fileId)
                .totalChunks(chunksCount)
                .startTime(LocalDateTime.now())
                .processedChunks(new AtomicInteger(0))
                .build();
                
            activeProcessings.put(fileId, metrics);
            log.info("Созданы предварительные метрики для документа {} с {} чанками", fileId, chunksCount);
        } else {
            // Если документ уже в обработке, обновляем количество чанков
            ProcessingMetrics metrics = activeProcessings.get(fileId);
            metrics.setTotalChunks(chunksCount);
            log.info("Обновлены метрики для документа {}: теперь {} чанков", fileId, chunksCount);
        }
    }

    public void startProcessing(String fileId, int totalChunks) {
        // Проверяем, есть ли уже метрики для этого документа
        if (activeProcessings.containsKey(fileId)) {
            ProcessingMetrics existingMetrics = activeProcessings.get(fileId);
            // Обновляем только если количество чанков изменилось
            if (existingMetrics.getTotalChunks() != totalChunks) {
                existingMetrics.setTotalChunks(totalChunks);
                log.info("Обновлены существующие метрики для документа {}: теперь {} блоков", 
                    fileId, totalChunks);
            }
            return;
        }
        
        // Если метрик еще нет, создаем новые
        ProcessingMetrics metrics = ProcessingMetrics.builder()
            .fileId(fileId)
            .totalChunks(totalChunks)
            .startTime(LocalDateTime.now())
            .processedChunks(new AtomicInteger(0))
            .build();
            
        activeProcessings.put(fileId, metrics);
        
        // Обновляем оценочное количество чанков в документе
        try {
            Optional<ProcessedDocument> docOpt = documentRepository.findByFileId(fileId);
            if (docOpt.isPresent()) {
                ProcessedDocument document = docOpt.get();
                document.setEstimatedChunks(totalChunks);
                documentRepository.save(document);
            }
        } catch (Exception e) {
            log.error("Ошибка обновления estimatedChunks для документа {}", fileId, e);
        }
        
        log.info("Начат мониторинг обработки файла {} с {} блоками", fileId, totalChunks);
    }

    public void chunkProcessed(String fileId) {
        ProcessingMetrics metrics = activeProcessings.get(fileId);
        if (metrics != null) {
            int processed = metrics.getProcessedChunks().incrementAndGet();
            
            // Если обработано больше чанков, чем было оценено изначально,
            // автоматически увеличиваем общее количество чанков
            if (processed > metrics.getTotalChunks()) {
                metrics.setTotalChunks(processed);
                log.info("Автоматически увеличено общее количество чанков для документа {}: {}", 
                    fileId, processed);
                
                // Обновляем оценочное количество чанков в документе
                try {
                    Optional<ProcessedDocument> docOpt = documentRepository.findByFileId(fileId);
                    if (docOpt.isPresent()) {
                        ProcessedDocument document = docOpt.get();
                        document.setEstimatedChunks(processed);
                        documentRepository.save(document);
                    }
                } catch (Exception e) {
                    log.error("Ошибка обновления estimatedChunks для документа {}", fileId, e);
                }
            }
            
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
                (double) metrics.getTotalChunks() / (duration.toMillis() / 1000.0));
        }
    }

    public ProcessingStatus getInitialProcessingStatus(String fileId) {
        // Сначала проверяем активные обработки
        ProcessingMetrics metrics = activeProcessings.get(fileId);
        if (metrics != null) {
            return getProcessingStatus(fileId);
        }
        
        // Если активной обработки нет, проверяем сохраненную информацию о документе
        try {
            Optional<ProcessedDocument> docOpt = documentRepository.findByFileId(fileId);
            if (docOpt.isPresent()) {
                ProcessedDocument document = docOpt.get();
                Integer estimatedChunks = document.getEstimatedChunks();
                
                // Если в документе есть оценка чанков, используем её
                if (estimatedChunks != null && estimatedChunks > 0) {
                    log.info("Для файла {} используем сохраненное количество блоков: {}", 
                        fileId, estimatedChunks);
                    
                    return ProcessingStatus.builder()
                        .fileId(fileId)
                        .totalChunks(estimatedChunks)
                        .processedChunks(0)
                        .progress(0.0)
                        .startTime(document.getProcessingStartedAt() != null ? 
                            document.getProcessingStartedAt() : LocalDateTime.now())
                        .build();
                }
            }
        } catch (Exception e) {
            log.error("Ошибка получения информации о документе {}", fileId, e);
        }
        
        // Если нет ни активной обработки, ни сохраненной информации - используем оценку
        int defaultEstimatedChunks = 22; // Среднее количество блоков в документе
        log.info("Для файла {} отсутствуют метрики, используем оценочное количество блоков: {}", 
            fileId, defaultEstimatedChunks);
        
        return ProcessingStatus.builder()
            .fileId(fileId)
            .totalChunks(defaultEstimatedChunks)
            .processedChunks(0)
            .progress(0.0)
            .startTime(LocalDateTime.now())
            .build();
    }

    public ProcessingStatus getProcessingStatus(String fileId) {
        ProcessingMetrics metrics = activeProcessings.get(fileId);
        if (metrics == null) {
            return null;
        }

        int processed = metrics.getProcessedChunks().get();
        int totalChunks = metrics.getTotalChunks();
        
        // Убедимся, что общее количество чанков не меньше обработанных
        if (processed > totalChunks) {
            totalChunks = processed;
            metrics.setTotalChunks(totalChunks);
            log.debug("Скорректировано общее количество чанков для {}: {} -> {}", 
                fileId, metrics.getTotalChunks(), totalChunks);
        }
        
        double progress = (double) processed / totalChunks * 100;
        
        return ProcessingStatus.builder()
            .fileId(fileId)
            .totalChunks(totalChunks)
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