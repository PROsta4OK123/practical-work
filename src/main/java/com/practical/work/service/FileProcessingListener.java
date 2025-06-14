package com.practical.work.service;

import com.practical.work.entity.FileProcessingQueue;
import com.practical.work.event.FileQueuedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class FileProcessingListener {

    @Autowired
    private FileQueueService fileQueueService;

    @Autowired
    private DocumentProcessingService documentProcessingService;

    // Множество для отслеживания активных обработок
    private final ConcurrentHashMap<String, Integer> activeProcessings = new ConcurrentHashMap<>();

    @EventListener
    public void handleFileQueuedEvent(FileQueuedEvent event) {
        log.info("📥 Получено событие для файла: {}. Начинаем проверку ресурсов.", event.getFileId());
        processNextInQueue();
    }

    private void processNextInQueue() {
        if (!hasAvailableResources()) {
            log.info("🚦 Все ресурсы заняты. Новые задачи будут ждать освобождения.");
            return;
        }

        Optional<FileProcessingQueue> nextFile = fileQueueService.getNextForProcessing();

        if (nextFile.isPresent()) {
            FileProcessingQueue file = nextFile.get();
            if (canProcessFile(file.getEstimatedThreads())) {
                processFileAsync(file);
            } else {
                log.debug("⏳ Недостаточно ресурсов для файла {} (нужно {} потоков)",
                        file.getOriginalFilename(), file.getEstimatedThreads());
            }
        } else {
            log.debug("📭 Очередь пуста - ожидаем новых файлов");
        }
    }

    private boolean hasAvailableResources() {
        int totalUsedThreads = activeProcessings.values().stream().mapToInt(Integer::intValue).sum();
        // TODO: Перенести в конфигурацию
        return totalUsedThreads < 6;
    }

    private boolean canProcessFile(int requiredThreads) {
        int totalUsedThreads = activeProcessings.values().stream().mapToInt(Integer::intValue).sum();
        // TODO: Перенести в конфигурацию
        return (totalUsedThreads + requiredThreads) <= 6;
    }

    private void processFileAsync(FileProcessingQueue queueItem) {
        String fileId = queueItem.getFileId();
        int threadsUsed = queueItem.getEstimatedThreads();

        activeProcessings.put(fileId, threadsUsed);

        log.info("🚀 Начинаем обработку файла: {} (ID: {}, потоков: {}). Активных обработок: {}",
                queueItem.getOriginalFilename(), fileId, threadsUsed, activeProcessings.size());

        CompletableFuture.runAsync(() -> {
            try {
                documentProcessingService.processDocument(
                        queueItem.getFilePath(),
                        fileId
                );
                fileQueueService.markAsCompleted(fileId);
                log.info("✅ Файл {} успешно обработан", queueItem.getOriginalFilename());

            } catch (Exception e) {
                log.error("❌ Ошибка обработки файла {} из очереди",
                        queueItem.getOriginalFilename(), e);
                fileQueueService.markAsFailed(fileId, e.getMessage());

            } finally {
                activeProcessings.remove(fileId);
                log.info("🔄 Обработка {} завершена. Активных обработок: {}. Проверяем очередь...",
                        queueItem.getOriginalFilename(), activeProcessings.size());

                // Проверяем, не освободилось ли место для следующего файла
                processNextInQueue();
            }
        });
    }

    public int getActiveProcessingCount() {
        return activeProcessings.size();
    }

    public int getTotalUsedThreads() {
        return activeProcessings.values().stream().mapToInt(Integer::intValue).sum();
    }
} 