package com.practical.work.service;

import com.practical.work.dto.FormattingResult;
import com.practical.work.dto.TextChunk;
import com.practical.work.dto.IndexedFormattingResult;
import com.practical.work.model.ProcessedDocument;
import com.practical.work.repository.ProcessedDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Optional;

@Service
@Slf4j
public class DocumentProcessingService {

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.file.processed-dir:processed}")
    private String processedDir;

    @Autowired
    private AiFormattingService aiFormattingService;
    
    @Autowired
    private ProcessingMetricsService metricsService;

    @Autowired
    private UkrainianAcademicFormattingService academicFormattingService;

    @Autowired
    private ProcessedDocumentRepository documentRepository;

    public CompletableFuture<String> processDocument(String inputFilePath, String fileId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Начало обработки документа: {}", inputFilePath);
                
                // Загрузка документа
                XWPFDocument document = new XWPFDocument(new FileInputStream(inputFilePath));
                List<XWPFParagraph> paragraphs = document.getParagraphs();
                
                log.info("Документ загружен, найдено {} абзацев", paragraphs.size());
                
                // Получаем размер файла для оценки сложности обработки
                long fileSizeBytes = new File(inputFilePath).length();
                
                // Создаем текстовые чанки для обработки
                List<TextChunk> textChunks = createTextChunks(paragraphs);
                
                if (textChunks.isEmpty()) {
                    log.warn("Документ не содержит текста для обработки");
                    return saveDocument(document, fileId);
                }
                
                int actualChunksCount = textChunks.size();
                log.info("Создано {} блоков для обработки", actualChunksCount);
                
                // Начинаем мониторинг обработки с точным количеством чанков
                metricsService.startProcessing(fileId, actualChunksCount);

                // Параллельная обработка всех блоков с ограниченным количеством потоков
                List<IndexedFormattingResult> formattingResults = 
                    aiFormattingService.formatTextChunks(textChunks, fileId, fileSizeBytes).get();

                // Применение результатов форматирования в правильном порядке
                applyFormattingResults(paragraphs, formattingResults);

                // Сохранение промежуточного документа после AI форматирования
                String aiFormattedFilePath = saveDocument(document, fileId + "_ai_formatted");
                
                // Применение украинских академических стандартов форматирования
                String outputFilePath = applyUkrainianAcademicFormatting(aiFormattedFilePath, fileId);
                
                // Завершаем мониторинг обработки
                metricsService.finishProcessing(fileId);
                
                log.info("Документ успешно обработан в многопоточном режиме: {}", outputFilePath);
                return outputFilePath;
                
            } catch (Exception e) {
                log.error("Ошибка многопоточной обработки документа: {}", inputFilePath, e);
                throw new RuntimeException("Ошибка обработки документа", e);
            }
        });
    }

    private List<TextChunk> createTextChunks(List<XWPFParagraph> paragraphs) {
        List<TextChunk> chunks = new ArrayList<>();
        int index = 0;
        
        for (int i = 0; i < paragraphs.size(); i++) {
            XWPFParagraph paragraph = paragraphs.get(i);
            String text = paragraph.getText();
            
            // Обрабатываем все абзацы, включая пустые
            if (text == null) {
                text = "";
            }
            
            String paragraphType = detectParagraphType(text, paragraph);
            
            // Создаем чанк для каждого абзаца (включая пустые для сохранения структуры)
                chunks.add(TextChunk.builder()
                    .index(index++)
                    .text(text)
                    .paragraphId("paragraph_" + i)
                    .build());
        }
        
        return chunks;
    }
    
    /**
     * Определение типа абзаца для лучшего форматирования
     */
    private String detectParagraphType(String text, XWPFParagraph paragraph) {
        if (text == null || text.trim().isEmpty()) {
            return "empty";
        }
        
        String trimmed = text.trim();
        
        // Заголовки
        if (isHeading(trimmed, paragraph)) {
            return "heading";
        }
        
        // Нумерованные списки (1., 2., а), б), i), ii))
        if (trimmed.matches("^\\d+[.)]\\s+.*") || 
            trimmed.matches("^[а-яієїёЁ][.)]\\s+.*") ||
            trimmed.matches("^[ivxlcdm]+[.)]\\s+.*")) {
            return "numbered_list";
        }
        
        // Маркированные списки
        if (trimmed.startsWith("•") || 
            trimmed.startsWith("-") || 
            trimmed.startsWith("*") ||
            trimmed.matches("^[•▪▫▬►◆○●]\\s+.*")) {
            return "bullet_list";
        }
        
        // Обычный абзац
        return "paragraph";
    }
    
    /**
     * Улучшенное определение заголовков
     */
    private boolean isHeading(String text, XWPFParagraph paragraph) {
        if (text == null || text.trim().isEmpty()) return false;
        
        // Проверяем по стилю
        String style = paragraph.getStyle();
        if (style != null && style.toLowerCase().contains("heading")) {
            return true;
        }
        
        String trimmed = text.trim();
        
        // Короткие строки в верхнем регистре
        if (trimmed.length() < 100 && trimmed.equals(trimmed.toUpperCase()) && 
            trimmed.matches(".*[А-ЯІЄЇЁA-Z].*")) {
            return true;
        }
        
        // Нумерованные заголовки
        if (trimmed.matches("^\\d+\\.\\s*[А-ЯІЄЇЁA-Z].*") ||
            trimmed.matches("^[IVXLCDM]+\\.\\s*[А-ЯІЄЇЁA-Z].*")) {
            return true;
        }
        
        return false;
    }

    private void applyFormattingResults(List<XWPFParagraph> paragraphs, 
                                       List<IndexedFormattingResult> formattingResults) {
        
        // Создаем карту соответствия paragraphId -> результат форматирования
        Map<String, IndexedFormattingResult> resultMap = new HashMap<>();
        for (IndexedFormattingResult result : formattingResults) {
            resultMap.put(result.getParagraphId(), result);
        }
        
        // Применяем форматирование к абзацам в том же порядке
        for (int i = 0; i < paragraphs.size(); i++) {
            XWPFParagraph paragraph = paragraphs.get(i);
            String paragraphId = "paragraph_" + i;
            
            IndexedFormattingResult result = resultMap.get(paragraphId);
            
            if (result != null && result.isSuccess() && result.getFormattingResult() != null) {
                log.debug("Применение форматирования к абзацу {} (индекс {})", 
                    paragraphId, result.getIndex());
                applyFormatting(paragraph, result.getFormattingResult());
            } else if (result != null && !result.isSuccess()) {
                log.warn("Ошибка форматирования абзаца {}: {}", 
                    paragraphId, result.getErrorMessage());
            } else {
                // Для абзацев без результата форматирования (например, пустых)
                // применяем базовое очистительное форматирование
                clearParagraphFormatting(paragraph);
            }
        }
    }

    private String applyUkrainianAcademicFormatting(String aiFormattedFilePath, String fileId) throws IOException {
        log.info("Применение украинских академических стандартов к документу: {}", fileId);
        
        String outputFileName = "formatted_" + fileId + ".docx";
        String outputFilePath = Paths.get(processedDir, outputFileName).toString();
        
        // Применяем академическое форматирование поверх AI форматирования
        academicFormattingService.formatDocumentToUkrainianStandards(aiFormattedFilePath, outputFilePath);
        
        // Удаляем промежуточный файл
        try {
            Files.deleteIfExists(Paths.get(aiFormattedFilePath));
        } catch (Exception e) {
            log.warn("Не удалось удалить промежуточный файл: {}", aiFormattedFilePath);
        }
        
        log.info("Документ отформатирован по украинским академическим стандартам: {}", outputFilePath);
        return outputFilePath;
    }

    private String saveDocument(XWPFDocument document, String fileId) throws IOException {
        String outputFileName = "formatted_" + fileId + ".docx";
        String outputFilePath = Paths.get(processedDir, outputFileName).toString();
        
        try (FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
            document.write(outputStream);
        }
        
        document.close();
        return outputFilePath;
    }

    /**
     * Очистка форматирования абзаца (для пустых абзацев и абзацев без AI форматирования)
     */
    private void clearParagraphFormatting(XWPFParagraph paragraph) {
        try {
            // Убираем подчеркивание со всех ранов в абзаце
            for (XWPFRun run : paragraph.getRuns()) {
                run.setUnderline(UnderlinePatterns.NONE);
            }
            log.debug("Очищено форматирование для абзаца: {}", paragraph.getText());
        } catch (Exception e) {
            log.warn("Ошибка очистки форматирования абзаца", e);
        }
    }

    private void applyFormatting(XWPFParagraph paragraph, FormattingResult formattingResult) {
        try {
            // Очистка существующих рунов
            for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
                paragraph.removeRun(i);
            }

            // Создание нового рана с форматированием
            XWPFRun run = paragraph.createRun();
            run.setText(formattingResult.getFormattedText());

            // Сброс всех стилей форматирования
            run.setBold(false);
            run.setItalic(false);
            run.setUnderline(UnderlinePatterns.NONE);

            // Применение стиля шрифта
            switch (formattingResult.getFontStyle().toLowerCase()) {
                case "bold":
                    run.setBold(true);
                    break;
                case "italic":
                    run.setItalic(true);
                    break;
                case "normal":
                default:
                    // Уже сброшено выше
                    break;
            }

            // Применение размера шрифта
            if (formattingResult.getFontSize() != null) {
                run.setFontSize(formattingResult.getFontSize());
            }

            // Применение выравнивания
            switch (formattingResult.getAlignment().toLowerCase()) {
                case "center":
                    paragraph.setAlignment(ParagraphAlignment.CENTER);
                    break;
                case "right":
                    paragraph.setAlignment(ParagraphAlignment.RIGHT);
                    break;
                case "justify":
                    paragraph.setAlignment(ParagraphAlignment.BOTH);
                    break;
                case "left":
                default:
                    paragraph.setAlignment(ParagraphAlignment.LEFT);
                    break;
            }

            // Применение специального форматирования в зависимости от типа
            switch (formattingResult.getFormattingType().toLowerCase()) {
                case "header":
                    // Дополнительное форматирование для заголовков
                    run.setBold(true);
                    paragraph.setSpacingAfter(240); // Увеличенный отступ после заголовка
                    break;
                case "list":
                    // Форматирование для списков
                    paragraph.setIndentationLeft(360); // Отступ слева
                    break;
                case "paragraph":
                default:
                    // Стандартное форматирование абзаца
                    paragraph.setSpacingAfter(120);
                    break;
            }

        } catch (Exception e) {
            log.error("Ошибка применения форматирования к абзацу", e);
        }
    }

    public String extractTextFromDocument(String filePath) {
        try {
            File file = new File(filePath);
            XWPFDocument document = new XWPFDocument(new FileInputStream(file));
            
            StringBuilder textBuilder = new StringBuilder();
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            
            for (XWPFParagraph paragraph : paragraphs) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    textBuilder.append(text).append("\n");
                }
            }
            
            document.close();
            return textBuilder.toString();
            
        } catch (Exception e) {
            log.error("Ошибка извлечения текста из документа: {}", filePath, e);
            throw new RuntimeException("Ошибка извлечения текста из документа", e);
        }
    }

    public boolean isValidWordDocument(String fileName) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        return "doc".equals(extension) || "docx".equals(extension);
    }

    public long getFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (Exception e) {
            log.error("Ошибка получения размера файла: {}", filePath, e);
            return 0;
        }
    }

    /**
     * Обновляет оценочное количество чанков в документе
     */
    private void updateDocumentEstimatedChunks(String fileId, int actualChunks) {
        try {
            Optional<ProcessedDocument> docOpt = documentRepository.findByFileId(fileId);
            if (docOpt.isPresent()) {
                ProcessedDocument document = docOpt.get();
                document.setEstimatedChunks(actualChunks);
                documentRepository.save(document);
                log.info("Обновлено оценочное количество чанков для документа {}: {}", fileId, actualChunks);
            }
        } catch (Exception e) {
            log.error("Ошибка обновления оценочного количества чанков для документа {}", fileId, e);
        }
    }

    /**
     * Оценивает количество чанков на основе размера файла и количества абзацев
     */
    public int estimateChunksCount(String filePath, int paragraphCount) {
        try {
            // Получаем размер файла
            long fileSizeBytes = new File(filePath).length();
            
            // Базовая оценка на основе размера файла
            int estimatedChunks;
            
            if (fileSizeBytes < 50 * 1024) { // < 50 KB
                estimatedChunks = Math.max(5, paragraphCount);
            } else if (fileSizeBytes < 200 * 1024) { // < 200 KB
                estimatedChunks = Math.max(10, paragraphCount);
            } else if (fileSizeBytes < 500 * 1024) { // < 500 KB
                estimatedChunks = Math.max(15, paragraphCount);
            } else if (fileSizeBytes < 1024 * 1024) { // < 1 MB
                estimatedChunks = Math.max(20, paragraphCount);
            } else if (fileSizeBytes < 3 * 1024 * 1024) { // < 3 MB
                estimatedChunks = Math.max(30, paragraphCount);
            } else {
                // Для больших файлов используем формулу на основе размера
                estimatedChunks = Math.max(40, (int)(fileSizeBytes / (30 * 1024)));
            }
            
            log.info("Оценка количества чанков для файла {} (размер: {} KB): {}", 
                filePath, fileSizeBytes/1024, estimatedChunks);
            
            return estimatedChunks;
            
        } catch (Exception e) {
            log.error("Ошибка оценки количества чанков для файла {}", filePath, e);
            return 22; // Возвращаем среднее значение по умолчанию
        }
    }

    /**
     * Метод для предварительного анализа документа и создания чанков
     * Используется для оценки прогресса до начала обработки
     */
    public List<TextChunk> createTextChunksPreview(List<XWPFParagraph> paragraphs) {
        log.info("Предварительное разделение документа на чанки, найдено {} абзацев", paragraphs.size());
        return createTextChunks(paragraphs);
    }
} 