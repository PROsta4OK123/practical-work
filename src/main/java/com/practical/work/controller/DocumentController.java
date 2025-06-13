package com.practical.work.controller;

import com.practical.work.dto.DocumentUploadResponse;
import com.practical.work.entity.FileProcessingQueue;
import com.practical.work.model.ProcessedDocument;
import com.practical.work.model.User;
import com.practical.work.service.AuthService;
import com.practical.work.service.DocumentService;
import com.practical.work.service.FileQueueService;
import com.practical.work.service.ProcessingMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("")
@CrossOrigin(origins = "*")
@Slf4j
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private AuthService authService;
    
    @Autowired
    private ProcessingMetricsService metricsService;
    
    @Autowired
    private FileQueueService fileQueueService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Проверка авторизации
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Токен авторизации обязателен"));
            }

            String token = authHeader.substring(7);
            User user = authService.getUserFromToken(token);

            // Загрузка документа
            DocumentUploadResponse response = documentService.uploadDocument(file, user);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Ошибка загрузки документа", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(DocumentUploadResponse.builder()
                    .success(false)
                    .message("Внутренняя ошибка сервера: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/format-document")
    public ResponseEntity<?> formatDocument(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Проверка авторизации
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Токен авторизации обязателен"));
            }

            String token = authHeader.substring(7);
            User user = authService.getUserFromToken(token);

            // Загрузка документа
            DocumentUploadResponse uploadResponse = documentService.uploadDocument(file, user);
            
            if (!uploadResponse.isSuccess()) {
                return ResponseEntity.badRequest().body(uploadResponse);
            }

            // Запуск обработки документа
            documentService.processDocument(uploadResponse.getFileId(), user)
                .thenAccept(success -> {
                    if (success) {
                        log.info("Документ {} обработан успешно", uploadResponse.getFileId());
                    } else {
                        log.error("Ошибка обработки документа {}", uploadResponse.getFileId());
                    }
                });

            return ResponseEntity.ok(Map.of(
                "success", true,
                "fileId", uploadResponse.getFileId(),
                "message", "Документ загружен и поставлен в очередь на обработку"
            ));

        } catch (Exception e) {
            log.error("Ошибка форматирования документа", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Внутренняя ошибка сервера: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<?> downloadDocument(
            @PathVariable String fileId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Проверка авторизации
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Токен авторизации обязателен"));
            }

            String token = authHeader.substring(7);
            User user = authService.getUserFromToken(token);

            // Получение информации о документе
            Optional<ProcessedDocument> documentOpt = documentService.getDocument(fileId);
            if (documentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ProcessedDocument document = documentOpt.get();

            // Проверка, что документ принадлежит пользователю
            if (!document.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Доступ запрещен"));
            }

            // Проверка статуса обработки
            if (document.getStatus() != ProcessedDocument.ProcessingStatus.COMPLETED) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Документ еще не обработан или обработка завершилась с ошибкой"));
            }

            // Получение файла
            File file = documentService.getProcessedFile(fileId);
            FileSystemResource resource = new FileSystemResource(file);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"formatted-" + document.getOriginalFilename() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, 
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

            return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);

        } catch (Exception e) {
            log.error("Ошибка скачивания документа {}", fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ошибка скачивания файла: " + e.getMessage()));
        }
    }

    @GetMapping("/document/{fileId}/status")
    public ResponseEntity<?> getDocumentStatus(
            @PathVariable String fileId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Проверка авторизации
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Токен авторизации обязателен"));
            }

            String token = authHeader.substring(7);
            User user = authService.getUserFromToken(token);

            // Получение информации о документе
            Optional<ProcessedDocument> documentOpt = documentService.getDocument(fileId);
            if (documentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ProcessedDocument document = documentOpt.get();

            // Проверка, что документ принадлежит пользователю
            if (document.getUser() == null || !document.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Доступ запрещен"));
            }

            // Используем HashMap для безопасной работы с null значениями
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("fileId", document.getFileId());
            responseBody.put("originalFilename", document.getOriginalFilename());
            responseBody.put("status", document.getStatus() != null ? document.getStatus().name().toLowerCase() : "unknown");
            responseBody.put("originalSize", document.getOriginalSize());
            responseBody.put("processedSize", document.getProcessedSize());
            responseBody.put("processingStartedAt", document.getProcessingStartedAt());
            responseBody.put("processingCompletedAt", document.getProcessingCompletedAt());
            responseBody.put("errorMessage", document.getErrorMessage());

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            log.error("Ошибка получения статуса документа {}: {}", fileId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Внутренняя ошибка сервера: " + e.getMessage()));
        }
    }

    @GetMapping("/document/{fileId}/progress")
    public ResponseEntity<?> getProcessingProgress(
            @PathVariable String fileId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Проверка авторизации
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Токен авторизации обязателен"));
            }

            String token = authHeader.substring(7);
            User user = authService.getUserFromToken(token);

            // Сначала проверяем, существует ли документ
            Optional<ProcessedDocument> documentOpt = documentService.getDocument(fileId);
            if (documentOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Документ не найден"));
            }

            ProcessedDocument document = documentOpt.get();

            // Проверка, что документ принадлежит пользователю
            if (!document.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "У вас нет доступа к этому документу"));
            }

            // Получаем статус обработки из сервиса метрик
            ProcessingMetricsService.ProcessingStatus status = 
                metricsService.getInitialProcessingStatus(fileId);
            
            // Проверяем активную обработку для получения актуальных данных
            ProcessingMetricsService.ProcessingStatus activeStatus = 
                metricsService.getProcessingStatus(fileId);
            
            if (activeStatus != null) {
                status = activeStatus;
            }

            // Вычисляем прогресс в процентах (максимум 100%)
            double progress = 0.0;
            if (status.getTotalChunks() > 0) {
                progress = Math.min(100.0, 
                    (double) status.getProcessedChunks() / status.getTotalChunks() * 100.0);
            }

            // Если документ уже обработан, возвращаем 100% прогресс
            if (document.getStatus() == ProcessedDocument.ProcessingStatus.COMPLETED) {
                Map<String, Object> response = new HashMap<>();
                response.put("fileId", fileId);
                response.put("status", document.getStatus().toString());
                response.put("progress", 100.0);
                response.put("processedChunks", status.getTotalChunks());
                response.put("totalChunks", status.getTotalChunks());
                
                return ResponseEntity.ok(response);
            }

            // Для документов в процессе обработки или ожидающих обработки
            // используем данные из сервиса метрик
            Map<String, Object> response = new HashMap<>();
            response.put("fileId", fileId);
            response.put("status", document.getStatus().toString());
            response.put("progress", progress);
            response.put("processedChunks", status.getProcessedChunks());
            response.put("totalChunks", status.getTotalChunks());
            
            // Добавляем информацию о времени обработки, если доступна
            if (status.getStartTime() != null) {
                response.put("startTime", status.getStartTime().toString());
                
                // Оценка оставшегося времени
                if (status.getProcessedChunks() > 0 && status.getTotalChunks() > 0) {
                    long elapsedSeconds = Duration.between(status.getStartTime(), LocalDateTime.now()).getSeconds();
                    double chunksPerSecond = (double) status.getProcessedChunks() / Math.max(1, elapsedSeconds);
                    int remainingChunks = status.getTotalChunks() - status.getProcessedChunks();
                    
                    if (chunksPerSecond > 0) {
                        long estimatedRemainingSeconds = (long) (remainingChunks / chunksPerSecond);
                        response.put("estimatedRemainingSeconds", estimatedRemainingSeconds);
                    }
                }
            }

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Ошибка получения статуса обработки документа", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ошибка получения статуса обработки документа"));
        }
    }

    @GetMapping("/queue/status")
    public ResponseEntity<?> getQueueStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Проверка авторизации
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Токен авторизации обязателен"));
            }

            String token = authHeader.substring(7);
            User user = authService.getUserFromToken(token);

            // Получение статистики очереди
            FileQueueService.QueueStatistics stats = fileQueueService.getQueueStatistics();
            
            // Получение документов пользователя из основной таблицы (ProcessedDocument)
            List<ProcessedDocument> userDocuments = documentService.getUserDocuments(user);

            return ResponseEntity.ok(Map.of(
                "queueStatistics", Map.of(
                    "pendingCount", stats.getPendingCount(),
                    "processingCount", stats.getProcessingCount(),
                    "totalInQueue", stats.getTotalInQueue()
                ),
                "userFiles", userDocuments.stream().map(doc -> {
                    Map<String, Object> docMap = new HashMap<>();
                    docMap.put("fileId", doc.getFileId());
                    docMap.put("originalFilename", doc.getOriginalFilename());
                    docMap.put("status", doc.getStatus().name().toLowerCase());
                    docMap.put("originalSize", doc.getOriginalSize() != null ? doc.getOriginalSize() : 0);
                    docMap.put("processedSize", doc.getProcessedSize() != null ? doc.getProcessedSize() : 0);
                    docMap.put("processingStartedAt", doc.getProcessingStartedAt());
                    docMap.put("processingCompletedAt", doc.getProcessingCompletedAt());
                    docMap.put("createdAt", doc.getCreatedAt());
                    docMap.put("errorMessage", doc.getErrorMessage());
                    return docMap;
                }).toList()
            ));

        } catch (Exception e) {
            log.error("Ошибка получения статуса очереди", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Внутренняя ошибка сервера: " + e.getMessage()));
        }
    }

    @GetMapping("/queue/processing-status")
    public ResponseEntity<?> getProcessingStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Проверка авторизации
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Токен авторизации обязателен"));
            }

            String token = authHeader.substring(7);
            User user = authService.getUserFromToken(token);

            // Получение детального статуса многофайловой обработки
            FileQueueService.QueueStatistics queueStats = fileQueueService.getQueueStatistics();
            ProcessingMetricsService.GlobalMetrics globalMetrics = metricsService.getGlobalMetrics();

            return ResponseEntity.ok(Map.of(
                "eventDrivenMode", true,
                "isCurrentlyProcessing", queueStats.getProcessingCount() > 0,
                "activeProcessings", globalMetrics.getActiveProcessings(),
                "totalUsedThreads", calculateUsedThreads(queueStats.getProcessingCount()),
                "maxAvailableThreads", 6,
                "queueStatistics", Map.of(
                    "pendingCount", queueStats.getPendingCount(),
                    "processingCount", queueStats.getProcessingCount(),
                    "completedCount", queueStats.getCompletedCount(),
                    "failedCount", queueStats.getFailedCount(),
                    "totalInQueue", queueStats.getTotalInQueue()
                ),
                "systemInfo", Map.of(
                    "mode", "Smart Resource Distribution",
                    "description", "Умное распределение ресурсов по размеру файлов",
                    "threadAllocation", Map.of(
                        "small", "Маленькие файлы (<1MB): 1 поток, до 6 файлов",
                        "medium", "Средние файлы (1-5MB): 2 потока, до 3 файлов",
                        "large", "Большие файлы (>5MB): 3 потока, до 2 файлов"
                    )
                )
            ));

        } catch (Exception e) {
            log.error("Ошибка получения статуса обработки", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Внутренняя ошибка сервера: " + e.getMessage()));
        }
    }

    @GetMapping("/processing/metrics")
    public ResponseEntity<?> getGlobalMetrics(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Проверка авторизации
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Токен авторизации обязателен"));
            }

            String token = authHeader.substring(7);
            User user = authService.getUserFromToken(token);

            // Получение глобальных метрик
            ProcessingMetricsService.GlobalMetrics metrics = 
                metricsService.getGlobalMetrics();

            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            log.error("Ошибка получения метрик обработки", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Внутренняя ошибка сервера: " + e.getMessage()));
        }
    }

    private int calculateUsedThreads(long processingCount) {
        // Упрощенная оценка: в среднем каждый файл использует 2 потока
        return (int) (processingCount * 2);
    }
} 