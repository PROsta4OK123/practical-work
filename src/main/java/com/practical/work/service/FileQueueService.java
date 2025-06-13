package com.practical.work.service;

import com.practical.work.entity.FileProcessingQueue;
import com.practical.work.entity.FileProcessingQueue.QueueStatus;
import com.practical.work.entity.FileProcessingQueue.Priority;
import com.practical.work.model.User;
import com.practical.work.repository.FileProcessingQueueRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Scope("singleton")
@Slf4j
public class FileQueueService {

    @Autowired
    private FileProcessingQueueRepository queueRepository;

    @Autowired
    private ProcessingMetricsService metricsService;

    // Размеры файлов для определения приоритета
    private static final long SMALL_FILE_THRESHOLD = 1024 * 1024; // 1 MB
    private static final long LARGE_FILE_THRESHOLD = 5 * 1024 * 1024; // 5 MB

    // Поля для кеширования статистики с thread-safe операциями
    private final AtomicReference<QueueStatistics> cachedStats = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> lastStatsUpdate = new AtomicReference<>();

    /**
     * Добавляет файл в очередь обработки
     */
    @Transactional
    public FileProcessingQueue addToQueue(String fileId, String originalFilename, 
                                        String filePath, long fileSizeBytes, User user) {
        
        // Определяем приоритет и количество потоков
        Priority priority = calculatePriority(fileSizeBytes);
        int estimatedThreads = calculateThreadsForFile(fileSizeBytes);

        FileProcessingQueue queueItem = FileProcessingQueue.builder()
            .fileId(fileId)
            .originalFilename(originalFilename)
            .filePath(filePath)
            .fileSizeBytes(fileSizeBytes)
            .user(user)
            .status(QueueStatus.PENDING)
            .priority(priority)
            .estimatedThreads(estimatedThreads)
            .retryCount(0)
            .build();

        FileProcessingQueue saved = queueRepository.save(queueItem);
        
        log.info("Файл {} добавлен в очередь обработки. Приоритет: {}, потоков: {}, позиция в очереди: {}", 
            originalFilename, priority, estimatedThreads, getQueuePosition(saved.getId()));
        
        return saved;
    }

    /**
     * Получает следующий файл для обработки
     */
    @Transactional
    public Optional<FileProcessingQueue> getNextForProcessing() {
        List<FileProcessingQueue> pendingFiles = queueRepository
            .findNextForProcessing(QueueStatus.PENDING);
        
        if (pendingFiles.isEmpty()) {
            return Optional.empty();
        }

        // Проверяем, можем ли мы обработать файл (есть ли свободные ресурсы)
        ProcessingMetricsService.GlobalMetrics metrics = metricsService.getGlobalMetrics();
        FileProcessingQueue nextFile = pendingFiles.get(0);
        
        // Проверяем, хватит ли ресурсов для обработки этого файла
        int requiredThreads = nextFile.getEstimatedThreads();
        int availableCapacity = metrics.getMaxConcurrentFiles() - metrics.getActiveProcessings();
        
        if (availableCapacity > 0) {
            // Начинаем обработку
            nextFile.setStatus(QueueStatus.PROCESSING);
            nextFile.setStartedAt(LocalDateTime.now());
            
            FileProcessingQueue updated = queueRepository.save(nextFile);
            log.info("Файл {} взят из очереди для обработки. Потребуется потоков: {}", 
                nextFile.getOriginalFilename(), requiredThreads);
            
            return Optional.of(updated);
        }
        
        log.debug("Файл {} ожидает в очереди - недостаточно ресурсов. Требуется: {}, доступно: {}", 
            nextFile.getOriginalFilename(), requiredThreads, availableCapacity);
        
        return Optional.empty();
    }

    /**
     * Отмечает файл как успешно обработанный
     */
    @Transactional
    public void markAsCompleted(String fileId) {
        Optional<FileProcessingQueue> queueItem = queueRepository.findByFileId(fileId);
        if (queueItem.isPresent()) {
            FileProcessingQueue item = queueItem.get();
            item.setStatus(QueueStatus.COMPLETED);
            queueRepository.save(item);
            
            log.info("Файл {} успешно обработан и удален из очереди", item.getOriginalFilename());
        }
    }

    /**
     * Отмечает файл как неудачно обработанный
     */
    @Transactional
    public void markAsFailed(String fileId, String errorMessage) {
        Optional<FileProcessingQueue> queueItem = queueRepository.findByFileId(fileId);
        if (queueItem.isPresent()) {
            FileProcessingQueue item = queueItem.get();
            item.setRetryCount(item.getRetryCount() + 1);
            item.setErrorMessage(errorMessage);
            
            // Если превышено количество попыток - помечаем как failed
            if (item.getRetryCount() >= 3) {
                item.setStatus(QueueStatus.FAILED);
                log.error("Файл {} помечен как неуспешно обработанный после {} попыток: {}", 
                    item.getOriginalFilename(), item.getRetryCount(), errorMessage);
            } else {
                // Возвращаем в очередь для повторной попытки
                item.setStatus(QueueStatus.PENDING);
                item.setStartedAt(null);
                log.warn("Файл {} возвращен в очередь для повторной обработки. Попытка {}/3. Ошибка: {}", 
                    item.getOriginalFilename(), item.getRetryCount(), errorMessage);
            }
            
            queueRepository.save(item);
        }
    }

    /**
     * Получает статус файла в очереди
     */
    public Optional<FileProcessingQueue> getQueueStatus(String fileId) {
        return queueRepository.findByFileId(fileId);
    }

    /**
     * Получает файлы пользователя в очереди
     */
    public List<FileProcessingQueue> getUserQueuedFiles(Long userId) {
        List<QueueStatus> activeStatuses = List.of(
            QueueStatus.PENDING, 
            QueueStatus.PROCESSING, 
            QueueStatus.COMPLETED,
            QueueStatus.FAILED
        );
        return queueRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(userId, activeStatuses);
    }

    /**
     * Получает позицию файла в очереди
     */
    public int getQueuePosition(Long queueId) {
        List<FileProcessingQueue> queue = queueRepository
            .findNextForProcessing(QueueStatus.PENDING);
        
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getId().equals(queueId)) {
                return i + 1;
            }
        }
        return -1; // Не найден в очереди
    }

    /**
     * Получает статистику очереди (оптимизированная версия с кешированием)
     */
    public synchronized QueueStatistics getQueueStatistics() {
        // Кешируем результат на 30 секунд чтобы не делать запросы каждые 5 секунд
        if (lastStatsUpdate.get() != null && 
            lastStatsUpdate.get().isAfter(LocalDateTime.now().minusSeconds(30))) {
            log.debug("Возвращаем кешированную статистику очереди");
            return cachedStats.get();
        }
        
        log.debug("Обновляем статистику очереди из базы данных");
        
        // Делаем запросы к базе данных только если кеш устарел
        long pending = queueRepository.countByStatus(QueueStatus.PENDING);
        long processing = queueRepository.countByStatus(QueueStatus.PROCESSING);
        long completed = queueRepository.countByStatus(QueueStatus.COMPLETED);
        long failed = queueRepository.countByStatus(QueueStatus.FAILED);

        QueueStatistics stats = QueueStatistics.builder()
            .pendingCount(pending)
            .processingCount(processing)
            .completedCount(completed)
            .failedCount(failed)
            .totalInQueue(pending + processing)
            .build();
            
        cachedStats.set(stats);
        lastStatsUpdate.set(LocalDateTime.now());
        log.debug("Статистика очереди обновлена: pending={}, processing={}, completed={}, failed={}", 
            pending, processing, completed, failed);
        return stats;
    }

    /**
     * Очистка старых записей (можно вызывать по расписанию)
     */
    @Transactional
    public void cleanupOldRecords() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        List<QueueStatus> completedStatuses = List.of(QueueStatus.COMPLETED, QueueStatus.FAILED);
        
        queueRepository.deleteOldRecords(completedStatuses, cutoffDate);
        log.info("Очищены старые записи очереди до {}", cutoffDate);
    }

    private Priority calculatePriority(long fileSizeBytes) {
        if (fileSizeBytes < SMALL_FILE_THRESHOLD) {
            return Priority.HIGH; // Маленькие файлы - высокий приоритет
        } else if (fileSizeBytes < LARGE_FILE_THRESHOLD) {
            return Priority.NORMAL; // Средние файлы - обычный приоритет
        } else {
            return Priority.LOW; // Большие файлы - низкий приоритет
        }
    }

    private int calculateThreadsForFile(long fileSizeBytes) {
        if (fileSizeBytes < SMALL_FILE_THRESHOLD) {
            return 1;
        } else if (fileSizeBytes < LARGE_FILE_THRESHOLD) {
            return 2;
        } else {
            return 3;
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class QueueStatistics {
        private long pendingCount;
        private long processingCount;
        private long completedCount;
        private long failedCount;
        private long totalInQueue;
    }
} 