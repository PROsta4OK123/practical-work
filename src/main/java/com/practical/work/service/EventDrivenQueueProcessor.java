package com.practical.work.service;

import com.practical.work.entity.FileProcessingQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class EventDrivenQueueProcessor {

    @Autowired
    private FileQueueService fileQueueService;

    @Autowired
    private DocumentProcessingService documentProcessingService;

    // Множество активных обработок (fileId -> потоки)
    private final ConcurrentHashMap<String, Integer> activeProcessings = new ConcurrentHashMap<>();
    private final AtomicBoolean isCheckingQueue = new AtomicBoolean(false);

    /**
     * Событийная обработка: проверяет очередь и запускает новые файлы если есть ресурсы
     */
    public void processNextInQueue() {
        // Проверяем, не идет ли уже проверка очереди
        if (isCheckingQueue.compareAndSet(false, true)) {
            log.debug("🔍 Проверяем очередь на наличие новых файлов...");
            
            try {
                // Продолжаем искать файлы пока есть свободные ресурсы
                while (hasAvailableResources()) {
                    Optional<FileProcessingQueue> nextFile = fileQueueService.getNextForProcessing();
                    
                    if (nextFile.isPresent()) {
                        FileProcessingQueue file = nextFile.get();
                        int requiredThreads = file.getEstimatedThreads();
                        
                        if (canProcessFile(requiredThreads)) {
                            processFileAsync(file);
                        } else {
                            log.debug("⏳ Недостаточно ресурсов для файла {} (нужно {} потоков)", 
                                file.getOriginalFilename(), requiredThreads);
                            break;
                        }
                    } else {
                        log.debug("📭 Очередь пуста - ожидаем новых файлов");
                        break;
                    }
                }
                
            } catch (Exception e) {
                log.error("❌ Ошибка при проверке очереди", e);
            } finally {
                isCheckingQueue.set(false);
            }
        } else {
            log.debug("⏳ Очередь уже проверяется");
        }
    }

    /**
     * Проверяет есть ли свободные ресурсы для обработки
     */
    private boolean hasAvailableResources() {
        int totalUsedThreads = activeProcessings.values().stream().mapToInt(Integer::intValue).sum();
        return totalUsedThreads < 6; // Максимум 6 потоков всего
    }

    /**
     * Проверяет можно ли обработать файл с требуемым количеством потоков
     */
    private boolean canProcessFile(int requiredThreads) {
        int totalUsedThreads = activeProcessings.values().stream().mapToInt(Integer::intValue).sum();
        return (totalUsedThreads + requiredThreads) <= 6;
    }

    /**
     * Асинхронная обработка файла с автоматическим переходом к следующему
     */
    private void processFileAsync(FileProcessingQueue queueItem) {
        String fileId = queueItem.getFileId();
        int threadsUsed = queueItem.getEstimatedThreads();
        
        // Регистрируем активную обработку
        activeProcessings.put(fileId, threadsUsed);
        
        log.info("🚀 Начинаем обработку файла: {} (ID: {}, потоков: {}). Активных обработок: {}", 
            queueItem.getOriginalFilename(), fileId, threadsUsed, activeProcessings.size());
        
        CompletableFuture.runAsync(() -> {
            try {
                // Вызываем обработку документа
                documentProcessingService.processDocument(
                    queueItem.getFilePath(), 
                    fileId
                );
                
                // Помечаем как успешно обработанный
                fileQueueService.markAsCompleted(fileId);
                
                log.info("✅ Файл {} успешно обработан", queueItem.getOriginalFilename());
                
            } catch (Exception e) {
                log.error("❌ Ошибка обработки файла {} из очереди", 
                    queueItem.getOriginalFilename(), e);
                
                // Помечаем как неуспешно обработанный
                fileQueueService.markAsFailed(fileId, e.getMessage());
                
            } finally {
                // ГЛАВНОЕ: Убираем файл из активных обработок
                activeProcessings.remove(fileId);
                
                log.info("🔄 Обработка {} завершена. Активных обработок: {}. Проверяем очередь...", 
                    queueItem.getOriginalFilename(), activeProcessings.size());
                
                // Рекурсивно вызываем проверку очереди для новых файлов
                processNextInQueue();
            }
        });
    }

    /**
     * Проверяет, идет ли сейчас обработка файлов
     */
    public boolean isCurrentlyProcessing() {
        return !activeProcessings.isEmpty();
    }
    
    /**
     * Получает количество активных обработок
     */
    public int getActiveProcessingCount() {
        return activeProcessings.size();
    }
    
    /**
     * Получает общее количество используемых потоков
     */
    public int getTotalUsedThreads() {
        return activeProcessings.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Принудительная остановка обработки (для экстренных случаев)
     */
    public void forceStop() {
        log.warn("⛔ Принудительная остановка обработки очереди");
        activeProcessings.clear();
    }
} 