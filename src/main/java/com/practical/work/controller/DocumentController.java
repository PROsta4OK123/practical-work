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
            if (!document.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Доступ запрещен"));
            }

            return ResponseEntity.ok(Map.of(
                "fileId", document.getFileId(),
                "originalFilename", document.getOriginalFilename(),
                "status", document.getStatus().name().toLowerCase(),
                "originalSize", document.getOriginalSize(),
                "processedSize", document.getProcessedSize(),
                "processingStartedAt", document.getProcessingStartedAt(),
                "processingCompletedAt", document.getProcessingCompletedAt(),
                "errorMessage", document.getErrorMessage()
            ));

        } catch (Exception e) {
            log.error("Ошибка получения статуса документа {}", fileId, e);
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

            // Получение прогресса обработки
            ProcessingMetricsService.ProcessingStatus progress = 
                metricsService.getProcessingStatus(fileId);
            
            if (progress == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(progress);

        } catch (Exception e) {
            log.error("Ошибка получения прогресса обработки документа {}", fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Внутренняя ошибка сервера: " + e.getMessage()));
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
            
            // Получение файлов пользователя в очереди
            List<FileProcessingQueue> userFiles = fileQueueService.getUserQueuedFiles(user.getId());

            return ResponseEntity.ok(Map.of(
                "queueStatistics", Map.of(
                    "pendingCount", stats.getPendingCount(),
                    "processingCount", stats.getProcessingCount(),
                    "totalInQueue", stats.getTotalInQueue()
                ),
                "userFiles", userFiles.stream().map(file -> Map.of(
                    "fileId", file.getFileId(),
                    "originalFilename", file.getOriginalFilename(),
                    "status", file.getStatus().name().toLowerCase(),
                    "priority", file.getPriority().name().toLowerCase(),
                    "queuePosition", fileQueueService.getQueuePosition(file.getId()),
                    "createdAt", file.getCreatedAt(),
                    "startedAt", file.getStartedAt(),
                    "estimatedThreads", file.getEstimatedThreads()
                )).toList()
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