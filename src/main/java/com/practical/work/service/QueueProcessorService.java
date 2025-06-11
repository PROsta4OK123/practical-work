package com.practical.work.service;

import com.practical.work.entity.FileProcessingQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class QueueProcessorService {

    @Autowired
    private FileQueueService fileQueueService;

    @Autowired
    private DocumentProcessingService documentProcessingService;

    /**
     * Планировщик для автоматической обработки файлов из очереди
     * Выполняется каждые 5 секунд
     */
    @Scheduled(fixedDelay = 5000) // 5 секунд
    public void processQueue() {
        try {
            // Получаем статистику очереди
            FileQueueService.QueueStatistics stats = fileQueueService.getQueueStatistics();
            
            if (stats.getPendingCount() > 0) {
                log.debug("В очереди обработки {} файлов, обрабатывается {}", 
                    stats.getPendingCount(), stats.getProcessingCount());
                
                // Пытаемся взять следующий файл для обработки
                Optional<FileProcessingQueue> nextFile = fileQueueService.getNextForProcessing();
                
                if (nextFile.isPresent()) {
                    processFileAsync(nextFile.get());
                } else {
                    log.debug("Нет доступных ресурсов для обработки новых файлов");
                }
            }
            
        } catch (Exception e) {
            log.error("Ошибка в планировщике обработки очереди", e);
        }
    }

    /**
     * Асинхронная обработка файла
     */
    private void processFileAsync(FileProcessingQueue queueItem) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Начинаем обработку файла из очереди: {} (ID: {})", 
                    queueItem.getOriginalFilename(), queueItem.getFileId());
                
                // Вызываем обработку документа
                documentProcessingService.processDocument(
                    queueItem.getFilePath(), 
                    queueItem.getFileId()
                );
                
                // Помечаем как успешно обработанный
                fileQueueService.markAsCompleted(queueItem.getFileId());
                
                log.info("Файл {} успешно обработан из очереди", queueItem.getOriginalFilename());
                
            } catch (Exception e) {
                log.error("Ошибка обработки файла {} из очереди", 
                    queueItem.getOriginalFilename(), e);
                
                // Помечаем как неуспешно обработанный
                fileQueueService.markAsFailed(queueItem.getFileId(), e.getMessage());
            }
        });
    }

    /**
     * Очистка старых записей - выполняется каждый час
     */
    @Scheduled(fixedRate = 3600000) // 1 час
    public void cleanupOldRecords() {
        try {
            fileQueueService.cleanupOldRecords();
            log.info("Выполнена очистка старых записей очереди");
        } catch (Exception e) {
            log.error("Ошибка при очистке старых записей", e);
        }
    }

    /**
     * Мониторинг зависших обработок - выполняется каждые 10 минут
     */
    @Scheduled(fixedRate = 600000) // 10 минут
    public void monitorStuckProcessing() {
        try {
            // Здесь можно добавить логику для мониторинга файлов, 
            // которые обрабатываются слишком долго
            FileQueueService.QueueStatistics stats = fileQueueService.getQueueStatistics();
            
            if (stats.getProcessingCount() > 0) {
                log.info("Мониторинг: обрабатывается файлов: {}, в очереди: {}", 
                    stats.getProcessingCount(), stats.getPendingCount());
            }
            
        } catch (Exception e) {
            log.error("Ошибка мониторинга зависших обработок", e);
        }
    }
} 