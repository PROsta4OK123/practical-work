package com.practical.work.service;

import com.practical.work.dto.FormattingResult;
import com.practical.work.dto.TextChunk;
import com.practical.work.dto.IndexedFormattingResult;
import com.practical.work.dto.ChunkQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.yaml.snakeyaml.Yaml;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;

// DJL imports для работы с GGUF моделями
import ai.djl.Model;
import ai.djl.engine.Engine;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorFactory;
import ai.djl.translate.TranslatorContext;
import ai.djl.training.util.ProgressBar;
import ai.djl.MalformedModelException;
import ai.djl.ndarray.NDList;

import java.io.File;
import java.nio.file.Paths;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AiFormattingService {

    @Value("${app.ai.model-path:src/main/resources/model/mistral-7b-instruct-v0.2.Q6_K.gguf}")
    private String modelPath;

    private Map<String, Object> promptConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Модель и предиктор для работы с GGUF
    private Model model;
    private Predictor<String, String> predictor;
    
    @Autowired
    private ProcessingMetricsService metricsService;
    
    private final WebClient webClient = WebClient.builder()
        .baseUrl("http://localhost:11434")
        .build();

    /**
     * Определяет количество потоков для обработки файла на основе его размера
     */
    private int calculateThreadsForFile(long fileSizeBytes, int totalChunks) {
        // Размеры файлов в байтах
        long smallFileThreshold = 1024 * 1024; // 1 MB
        long largeFileThreshold = 5 * 1024 * 1024; // 5 MB
        
        // Также учитываем количество чанков
        if (fileSizeBytes < smallFileThreshold || totalChunks <= 5) {
            return 1; // Маленькие файлы - 1 поток
        } else if (fileSizeBytes < largeFileThreshold || totalChunks <= 15) {
            return 2; // Средние файлы - 2 потока
        } else {
            return 3; // Большие файлы - максимум 3 потока
        }
    }

    /**
     * Распределяет чанки по очередям для обработки потоками
     */
    private List<ChunkQueue> distributeChunksToQueues(List<TextChunk> chunks, String fileId, int threadCount) {
        List<ChunkQueue> queues = new ArrayList<>();
        
        // Создаем пустые очереди
        for (int i = 0; i < threadCount; i++) {
            queues.add(ChunkQueue.builder()
                .queueId(i)
                .chunks(new ArrayList<>())
                .fileId(fileId)
                .build());
        }
        
        // Распределяем чанки по очередям round-robin
        for (int i = 0; i < chunks.size(); i++) {
            int queueIndex = i % threadCount;
            queues.get(queueIndex).getChunks().add(chunks.get(i));
        }
        
        log.info("Распределено {} чанков по {} очередям для файла {}", 
            chunks.size(), threadCount, fileId);
        
        return queues;
    }

    @PostConstruct
    public void init() {
        loadPromptConfig();
        initializeModel();
    }

    private void loadPromptConfig() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("ai-prompts.yaml")) {
            if (inputStream != null) {
                Yaml yaml = new Yaml();
                promptConfig = yaml.load(inputStream);
                log.info("Конфигурация промптов загружена успешно");
            } else {
                log.error("Файл ai-prompts.yaml не найден");
                throw new RuntimeException("Файл конфигурации промптов не найден");
            }
        } catch (Exception e) {
            log.error("Ошибка загрузки конфигурации промптов", e);
            throw new RuntimeException("Ошибка загрузки конфигурации промптов", e);
        }
    }

    private void initializeModel() {
        try {
            File modelFile = new File(modelPath);
            
            if (!modelFile.exists()) {
                log.warn("Файл модели не найден: {}. Используется режим симуляции.", modelPath);
                return;
            }
            
            log.info("Загрузка модели Mistral из файла: {}", modelPath);
            
            // Пока что используем проверенный подход с симуляцией
            // TODO: Интеграция с реальной моделью Mistral через DJL требует более сложной настройки
            log.info("Модель Mistral готова к использованию: {} (размер: {} MB)", 
                modelPath, modelFile.length() / (1024 * 1024));
            log.warn("Пока используется режим симуляции. Для подключения реальной модели требуется настройка DJL.");
            
        } catch (Exception e) {
            log.error("Критическая ошибка инициализации модели", e);
            throw new RuntimeException("Ошибка инициализации модели", e);
        }
    }

    public CompletableFuture<FormattingResult> formatText(String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("=== AI ОБРАБОТКА ===");
                log.info("ВХОДНОЙ ТЕКСТ: '{}'", text);
                
                // Получение промптов из конфигурации
                Map<String, Object> documentFormatting = (Map<String, Object>) promptConfig.get("document_formatting");
                String systemPrompt = (String) documentFormatting.get("system_prompt");
                String userPromptTemplate = (String) documentFormatting.get("user_prompt_template");
                
                // Формирование полного промпта
                String userPrompt = userPromptTemplate.replace("{text}", text);
                log.info("СФОРМИРОВАННЫЙ ПРОМПТ: '{}'", userPrompt);
                
                // Вызов реальной модели через Ollama API
                FormattingResult result = callOllamaApi(systemPrompt, userPrompt, text);
                
                log.info("ИТОГОВЫЙ РЕЗУЛЬТАТ: type={}, formatted_text='{}'", 
                    result.getFormattingType(), result.getFormattedText());
                log.info("=== КОНЕЦ AI ОБРАБОТКИ ===");
                return result;
                
            } catch (Exception e) {
                log.error("Ошибка форматирования текста", e);
                // Возвращаем исходный текст в случае ошибки
                return FormattingResult.builder()
                    .formattedText(text)
                    .formattingType("paragraph")
                    .fontStyle("normal")
                    .fontSize(12)
                    .alignment("justify")
                    .build();
            }
        });
    }

    /**
     * Обрабатывает очередь чанков в одном потоке
     */
    @Async("taskExecutor")
    public CompletableFuture<List<IndexedFormattingResult>> processChunkQueue(ChunkQueue queue) {
        return CompletableFuture.supplyAsync(() -> {
            List<IndexedFormattingResult> results = new ArrayList<>();
            String fileId = queue.getFileId();
            int queueId = queue.getQueueId();
            
            log.info("Поток {} начал обработку {} чанков для файла {}", 
                queueId, queue.getChunks().size(), fileId);
            
            for (TextChunk chunk : queue.getChunks()) {
                try {
                    log.debug("Поток {} обрабатывает блок {} с индексом {} для файла {}", 
                        queueId, chunk.getParagraphId(), chunk.getIndex(), fileId);
                    
                    // Реальная обработка через AI
                    FormattingResult formattingResult = formatText(chunk.getText()).get();
                    
                    // Уведомляем метрики об обработке блока
                    if (metricsService != null) {
                        metricsService.chunkProcessed(fileId);
                    }
                    
                    IndexedFormattingResult result = IndexedFormattingResult.builder()
                        .index(chunk.getIndex())
                        .paragraphId(chunk.getParagraphId())
                        .formattingResult(formattingResult)
                        .success(true)
                        .build();
                    
                    results.add(result);
                    
                    log.debug("Поток {} завершил обработку блока {} с индексом {} для файла {}", 
                        queueId, chunk.getParagraphId(), chunk.getIndex(), fileId);
                        
                } catch (Exception e) {
                    log.error("Поток {} ошибка обработки блока {} с индексом {} для файла {}", 
                        queueId, chunk.getParagraphId(), chunk.getIndex(), fileId, e);
                    
                    IndexedFormattingResult errorResult = IndexedFormattingResult.builder()
                        .index(chunk.getIndex())
                        .paragraphId(chunk.getParagraphId())
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build();
                    
                    results.add(errorResult);
                }
            }
            
            log.info("Поток {} завершил обработку {} чанков для файла {}", 
                queueId, queue.getChunks().size(), fileId);
            
            return results;
        });
    }

    @Async("taskExecutor")
    public CompletableFuture<IndexedFormattingResult> formatTextChunk(TextChunk chunk, String fileId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Начало обработки блока {} с индексом {} для файла {}", 
                    chunk.getParagraphId(), chunk.getIndex(), fileId);
                
                // Реальная обработка через AI
                FormattingResult result = formatText(chunk.getText()).get();
                
                // Уведомляем метрики об обработке блока
                if (metricsService != null) {
                    metricsService.chunkProcessed(fileId);
                }
                
                log.debug("Завершена обработка блока {} с индексом {} для файла {}", 
                    chunk.getParagraphId(), chunk.getIndex(), fileId);
                
                return IndexedFormattingResult.builder()
                    .index(chunk.getIndex())
                    .paragraphId(chunk.getParagraphId())
                    .formattingResult(result)
                    .success(true)
                    .build();
                    
            } catch (Exception e) {
                log.error("Ошибка обработки блока {} с индексом {} для файла {}", 
                    chunk.getParagraphId(), chunk.getIndex(), fileId, e);
                return IndexedFormattingResult.builder()
                    .index(chunk.getIndex())
                    .paragraphId(chunk.getParagraphId())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
            }
        });
    }

    public CompletableFuture<List<IndexedFormattingResult>> formatTextChunks(List<TextChunk> chunks, String fileId, long fileSizeBytes) {
        log.info("Начало обработки {} блоков текста для файла {} (размер: {} байт)", 
            chunks.size(), fileId, fileSizeBytes);
        
        // Определяем количество потоков для этого файла
        int threadCount = calculateThreadsForFile(fileSizeBytes, chunks.size());
        
        // Распределяем чанки по очередям
        List<ChunkQueue> queues = distributeChunksToQueues(chunks, fileId, threadCount);
        
        // Создаем асинхронные задачи для каждой очереди
        List<CompletableFuture<List<IndexedFormattingResult>>> queueFutures = queues.stream()
            .map(this::processChunkQueue)
            .toList();
        
        // Ждем завершения всех очередей
        CompletableFuture<Void> allQueuesComplete = CompletableFuture.allOf(
            queueFutures.toArray(new CompletableFuture[0])
        );
        
        return allQueuesComplete.thenApply(v -> {
            // Собираем результаты из всех очередей
            List<IndexedFormattingResult> allResults = new ArrayList<>();
            
            for (CompletableFuture<List<IndexedFormattingResult>> queueFuture : queueFutures) {
                allResults.addAll(queueFuture.join());
            }
            
            // Сортируем по индексу для сохранения порядка
            allResults.sort((r1, r2) -> Integer.compare(r1.getIndex(), r2.getIndex()));
                
            log.info("Завершена обработка {} блоков текста для файла {} с использованием {} потоков", 
                allResults.size(), fileId, threadCount);
            return allResults;
        });
    }

    /**
     * Вызывает Ollama API для форматирования текста
     */
    private FormattingResult callOllamaApi(String systemPrompt, String userPrompt, String text) {
        try {
            // Формируем запрос для Ollama API
            Map<String, Object> request = Map.of(
                "model", "mistral",
                "prompt", systemPrompt + "\n\n" + userPrompt,
                "stream", false
            );
            
            // Отправляем запрос к Ollama
            Map<String, Object> response = webClient
                .post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            if (response != null && response.containsKey("response")) {
                String aiResponse = (String) response.get("response");
                log.info("Ответ от Ollama API для текста '{}': {}", 
                    text.length() > 50 ? text.substring(0, 50) + "..." : text, aiResponse);
                return parseAiResponse(aiResponse, text);
            } else {
                log.warn("Пустой ответ от Ollama API, используем исходный текст");
                return FormattingResult.builder()
                    .formattedText(text)
                    .formattingType("paragraph")
                    .fontStyle("normal")
                    .fontSize(14)
                    .alignment("justify")
                    .build();
            }
            
        } catch (Exception e) {
            log.error("КРИТИЧЕСКАЯ ОШИБКА: Не удается связаться с Ollama API: {}", e.getMessage());
            log.error("Используем fallback форматирование для текста: '{}'", 
                text.length() > 100 ? text.substring(0, 100) + "..." : text);
            
            // В случае ошибки API используем fallback логику
            return createFallbackFormattingResult(text);
        }
    }
    
    /**
     * Парсит ответ от AI и создает FormattingResult
     */
    private FormattingResult parseAiResponse(String aiResponse, String originalText) {
        try {
            // Очищаем ответ от лишних символов и ищем JSON
            String cleanedResponse = extractJsonFromResponse(aiResponse);
            log.debug("Очищенный JSON: {}", cleanedResponse);
            
            // Пытаемся распарсить JSON ответ от AI
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> responseMap = mapper.readValue(cleanedResponse, Map.class);
            
            // Получаем значения с проверкой типов
            String formattedText = (String) responseMap.getOrDefault("formatted_text", originalText);
            String formattingType = (String) responseMap.getOrDefault("formatting_type", "paragraph");
            String fontStyle = (String) responseMap.getOrDefault("font_style", "normal");
            String alignment = (String) responseMap.getOrDefault("alignment", "justify");
            
            // Обрабатываем font_size который может быть строкой или числом
            int fontSize = 14;
            Object fontSizeObj = responseMap.get("font_size");
            if (fontSizeObj instanceof Number) {
                fontSize = ((Number) fontSizeObj).intValue();
            } else if (fontSizeObj instanceof String) {
                try {
                    fontSize = Integer.parseInt((String) fontSizeObj);
                } catch (NumberFormatException e) {
                    log.warn("Не удалось парсить font_size как число: {}", fontSizeObj);
                }
            }
            
            log.info("Успешно распарсен ответ AI: type={}, style={}, size={}, text='{}'", 
                formattingType, fontStyle, fontSize, 
                formattedText.length() > 50 ? formattedText.substring(0, 50) + "..." : formattedText);
        
        return FormattingResult.builder()
                .formattedText(formattedText)
            .formattingType(formattingType)
            .fontStyle(fontStyle)
            .fontSize(fontSize)
            .alignment(alignment)
            .build();
                
        } catch (Exception e) {
            log.error("ОШИБКА: Не удалось распарсить ответ AI как JSON: {}. Используем fallback.", e.getMessage());
            log.error("Проблемный ответ AI: '{}'", aiResponse);
            
            // FALLBACK: используем простую логику форматирования
            return createFallbackFormattingResult(originalText);
        }
    }
    
    /**
     * Создает результат форматирования с использованием простой логики (fallback)
     */
    private FormattingResult createFallbackFormattingResult(String text) {
        log.info("FALLBACK: Используем локальную логику для текста: '{}'", 
            text != null && text.length() > 50 ? text.substring(0, 50) + "..." : text);
            
        if (text == null || text.trim().isEmpty()) {
            return FormattingResult.builder()
                .formattedText("")
                .formattingType("empty")
                .fontStyle("normal")
                .fontSize(14)
                .alignment("left")
                .build();
        }
        
        String trimmed = text.trim();
        
        // Простая логика определения типа
        if (isLikelyHeader(trimmed)) {
            log.info("FALLBACK: Определен как ЗАГОЛОВОК: '{}'", trimmed);
            return FormattingResult.builder()
                .formattedText(trimmed)
                .formattingType("header")
                .fontStyle("bold")
                .fontSize(16)
                .alignment("left")
                .build();
        } else if (trimmed.matches("^[\\d\\-\\*•].*") || trimmed.matches("^\\d+\\..*")) {
            log.info("FALLBACK: Определен как СПИСОК: '{}'", trimmed);
            return FormattingResult.builder()
                .formattedText(trimmed)
                .formattingType("list")
                .fontStyle("normal")
                .fontSize(14)
                .alignment("justify")
                .build();
        } else {
            log.info("FALLBACK: Определен как АБЗАЦ: '{}'", trimmed);
            return FormattingResult.builder()
                .formattedText(trimmed)
                .formattingType("paragraph")
                .fontStyle("normal")
                .fontSize(14)
                .alignment("justify")
                .build();
        }
    }
    
    /**
     * Извлекает JSON из ответа AI, убирая лишний текст
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }
        
        String trimmed = response.trim();
        
        // Ищем первую открывающую скобку
        int startIndex = trimmed.indexOf('{');
        if (startIndex == -1) {
            log.warn("JSON объект не найден в ответе AI");
            return "{}";
        }
        
        // Ищем последнюю закрывающую скобку
        int endIndex = trimmed.lastIndexOf('}');
        if (endIndex == -1 || endIndex <= startIndex) {
            log.warn("Не найдена закрывающая скобка JSON в ответе AI");
            return "{}";
        }
        
        // Извлекаем JSON часть
        String jsonPart = trimmed.substring(startIndex, endIndex + 1);
        log.debug("Извлеченный JSON: {}", jsonPart);
        
        return jsonPart;
    }
    
    /**
     * Более умная логика определения заголовков для fallback
     */
    private boolean isLikelyHeader(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = text.trim();
        
        // Слишком длинные строки не заголовки  
        if (trimmed.length() > 80) {
            return false;
        }
        
        // Заголовки обычно:
        // 1. Начинаются с заглавной буквы
        // 2. Короткие (< 80 символов)
        // 3. НЕ заканчиваются точкой
        // 4. НЕ содержат много знаков препинания
        // 5. НЕ начинаются с цифр (это могут быть списки)
        
        boolean startsWithCapital = Character.isUpperCase(trimmed.charAt(0));
        boolean noEndingPeriod = !trimmed.endsWith(".");
        boolean shortText = trimmed.length() < 50;
        boolean fewPunctuation = (trimmed.length() - trimmed.replaceAll("[,.;!?:]", "").length()) < 3;
        boolean notStartsWithDigit = !Character.isDigit(trimmed.charAt(0));
        boolean notListItem = !trimmed.matches("^[\\-\\*•].*");
        
        // Это заголовок если выполняются большинство условий
        int score = 0;
        if (startsWithCapital) score++;
        if (noEndingPeriod) score++;
        if (shortText) score++;
        if (fewPunctuation) score++;
        if (notStartsWithDigit) score++;
        if (notListItem) score++;
        
        // Заголовок если score >= 4 из 6
        return score >= 4;
    }

} 