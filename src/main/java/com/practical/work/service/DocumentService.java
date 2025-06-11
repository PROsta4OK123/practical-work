package com.practical.work.service;

import com.practical.work.dto.DocumentUploadResponse;
import com.practical.work.entity.FileProcessingQueue;
import com.practical.work.model.ProcessedDocument;
import com.practical.work.model.User;
import com.practical.work.repository.ProcessedDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@Transactional
public class DocumentService {

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Autowired
    private ProcessedDocumentRepository documentRepository;

    @Autowired
    private DocumentProcessingService documentProcessingService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private ProcessingMetricsService metricsService;
    
    @Autowired
    private FileQueueService fileQueueService;

    @Autowired
    private EventDrivenQueueProcessor queueProcessor;

    public DocumentUploadResponse uploadDocument(MultipartFile file, User user) {
        try {
            log.info("Загрузка документа {} пользователем {}", file.getOriginalFilename(), user.getEmail());

            // Проверка файла
            if (file.isEmpty()) {
                return DocumentUploadResponse.builder()
                    .success(false)
                    .message("Файл пустой")
                    .build();
            }

            if (!documentProcessingService.isValidWordDocument(file.getOriginalFilename())) {
                return DocumentUploadResponse.builder()
                    .success(false)
                    .message("Неподдерживаемый тип файла. Поддерживаются только .doc и .docx")
                    .build();
            }

            // Проверка поинтов пользователя
            if (user.getPoints() < 1) {
                return DocumentUploadResponse.builder()
                    .success(false)
                    .message("Недостаточно поинтов для обработки документа")
                    .build();
            }

            // Убираем проверку загрузки системы - теперь файлы идут в очередь

            // Создание директории для загрузок если она не существует
            File uploadDirectory = new File(uploadDir);
            if (!uploadDirectory.exists()) {
                boolean created = uploadDirectory.mkdirs();
                log.info("Создание папки uploads: {} - {}", uploadDirectory.getAbsolutePath(), created);
            }

            // Генерация уникального идентификатора файла
            String fileId = UUID.randomUUID().toString();
            String fileName = fileId + "_" + file.getOriginalFilename();
            File uploadFile = new File(uploadDirectory, fileName);
            String filePath = uploadFile.getAbsolutePath();

            // Сохранение файла на диск
            file.transferTo(uploadFile);

            // Создание записи в базе данных
            ProcessedDocument document = ProcessedDocument.builder()
                .fileId(fileId)
                .originalFilename(file.getOriginalFilename())
                .originalFilePath(filePath)
                .originalSize(file.getSize())
                .status(ProcessedDocument.ProcessingStatus.UPLOADED)
                .user(user)
                .build();

            documentRepository.save(document);

            // Добавляем файл в очередь обработки
            FileProcessingQueue queueItem = fileQueueService.addToQueue(
                fileId, 
                file.getOriginalFilename(), 
                filePath, 
                file.getSize(), 
                user
            );

            // Получаем позицию в очереди
            int queuePosition = fileQueueService.getQueuePosition(queueItem.getId());
            String queueMessage = queuePosition > 0 ? 
                String.format(" Позиция в очереди: %d", queuePosition) : "";

            log.info("Документ загружен успешно и добавлен в очередь: {}", fileId);

            queueProcessor.processNextInQueue();

            return DocumentUploadResponse.builder()
                .success(true)
                .fileId(fileId)
                .originalName(file.getOriginalFilename())
                .size(file.getSize())
                .message("Файл загружен успешно и добавлен в очередь обработки." + queueMessage)
                .queuePosition(queuePosition > 0 ? queuePosition : null)
                .build();

        } catch (IOException e) {
            log.error("Ошибка загрузки файла", e);
            return DocumentUploadResponse.builder()
                .success(false)
                .message("Ошибка сохранения файла: " + e.getMessage())
                .build();
        } catch (Exception e) {
            log.error("Неожиданная ошибка при загрузке файла", e);
            return DocumentUploadResponse.builder()
                .success(false)
                .message("Внутренняя ошибка сервера")
                .build();
        }
    }

    public CompletableFuture<Boolean> processDocument(String fileId, User user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Начало обработки документа {} пользователем {}", fileId, user.getEmail());

                Optional<ProcessedDocument> docOpt = documentRepository.findByFileId(fileId);
                if (docOpt.isEmpty()) {
                    log.error("Документ не найден: {}", fileId);
                    return false;
                }

                ProcessedDocument document = docOpt.get();

                // Проверка, что документ принадлежит пользователю
                if (!document.getUser().getId().equals(user.getId())) {
                    log.error("Пользователь {} пытается обработать чужой документ {}", user.getEmail(), fileId);
                    return false;
                }

                // Проверка и списание поинтов
                if (!userService.usePoints(user, 1)) {
                    log.error("У пользователя {} недостаточно поинтов для обработки документа {}", user.getEmail(), fileId);
                    return false;
                }

                // Обновление статуса документа
                document.setStatus(ProcessedDocument.ProcessingStatus.PROCESSING);
                document.setProcessingStartedAt(LocalDateTime.now());
                documentRepository.save(document);

                // Обработка документа
                String processedFilePath = documentProcessingService
                    .processDocument(document.getOriginalFilePath(), fileId).get();

                // Обновление информации о документе
                document.setProcessedFilePath(processedFilePath);
                document.setProcessedSize(documentProcessingService.getFileSize(processedFilePath));
                document.setStatus(ProcessedDocument.ProcessingStatus.COMPLETED);
                document.setProcessingCompletedAt(LocalDateTime.now());
                documentRepository.save(document);

                log.info("Документ {} обработан успешно", fileId);
                return true;

            } catch (Exception e) {
                log.error("Ошибка обработки документа {}", fileId, e);

                // Обновление статуса документа в случае ошибки
                try {
                    Optional<ProcessedDocument> docOpt = documentRepository.findByFileId(fileId);
                    if (docOpt.isPresent()) {
                        ProcessedDocument document = docOpt.get();
                        document.setStatus(ProcessedDocument.ProcessingStatus.FAILED);
                        document.setErrorMessage(e.getMessage());
                        documentRepository.save(document);

                        // Возврат поинтов пользователю при ошибке
                        userService.addPoints(user, 1);
                    }
                } catch (Exception ex) {
                    log.error("Ошибка обновления статуса документа", ex);
                }

                return false;
            }
        });
    }

    public Optional<ProcessedDocument> getDocument(String fileId) {
        return documentRepository.findByFileId(fileId);
    }

    public List<ProcessedDocument> getUserDocuments(User user) {
        return documentRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public File getProcessedFile(String fileId) throws IOException {
        Optional<ProcessedDocument> docOpt = documentRepository.findByFileId(fileId);
        if (docOpt.isEmpty()) {
            throw new RuntimeException("Документ не найден");
        }

        ProcessedDocument document = docOpt.get();
        if (document.getStatus() != ProcessedDocument.ProcessingStatus.COMPLETED) {
            throw new RuntimeException("Документ еще не обработан");
        }

        File file = new File(document.getProcessedFilePath());
        if (!file.exists()) {
            throw new RuntimeException("Обработанный файл не найден на диске");
        }

        return file;
    }
} 