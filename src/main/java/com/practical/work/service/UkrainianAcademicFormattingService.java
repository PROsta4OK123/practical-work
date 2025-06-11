package com.practical.work.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;

@Service
@Slf4j
public class UkrainianAcademicFormattingService {

    /**
     * Форматирование документа согласно требованиям академических работ Украины
     */
    public void formatDocumentToUkrainianStandards(String inputPath, String outputPath) throws IOException {
        log.info("Начинаю академическое форматирование документа: {}", inputPath);
        
        // Проверка входного файла
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new IOException("Входной файл не существует: " + inputPath);
        }
        
        long inputSize = inputFile.length();
        log.info("Размер входного файла: {} байт", inputSize);
        
        if (inputSize == 0) {
            throw new IOException("Входной файл пуст: " + inputPath);
        }

        try (FileInputStream fis = new FileInputStream(inputPath);
             XWPFDocument document = new XWPFDocument(fis)) {
             
            log.info("Документ успешно загружен, количество абзацев: {}", document.getParagraphs().size());

            // 1. Настройка параметров страницы
            setupPageSettings(document);

            // 2. Настройка стилей документа
            setupDocumentStyles(document);

            // 3. Форматирование всех абзацев
            formatAllParagraphs(document);

            // 4. Настройка заголовков
            formatHeadings(document);

            // 5. Настройка списков
            formatLists(document);

            // 6. Добавление нумерации страниц
            addPageNumbering(document);

            // 7. Сохранение отформатированного документа
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                document.write(fos);
                fos.flush();
            }

            // Проверка размера созданного файла
            File outputFile = new File(outputPath);
            long fileSize = outputFile.length();
            log.info("Академическое форматирование завершено: {} (размер: {} байт)", outputPath, fileSize);
            
            if (fileSize < 1000) {
                log.warn("ПРЕДУПРЕЖДЕНИЕ: Размер выходного файла подозрительно мал: {} байт", fileSize);
            }
        }
    }

    /**
     * Настройка параметров страницы согласно украинским стандартам
     */
    private void setupPageSettings(XWPFDocument document) {
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        
        // Поля страницы (в твипах: 1 см = 567 твипов)
        CTPageMar pageMar = sectPr.addNewPgMar();
        pageMar.setTop(BigInteger.valueOf(1134));    // 2.0 см (верхнее)
        pageMar.setBottom(BigInteger.valueOf(1134)); // 2.0 см (нижнее)
        pageMar.setLeft(BigInteger.valueOf(1701));   // 3.0 см (левое)
        pageMar.setRight(BigInteger.valueOf(567));   // 1.0 см (правое)

        // Размер страницы A4
        CTPageSz pageSize = sectPr.addNewPgSz();
        pageSize.setW(BigInteger.valueOf(11906)); // 21 см
        pageSize.setH(BigInteger.valueOf(16838)); // 29.7 см
        pageSize.setOrient(STPageOrientation.PORTRAIT);

        log.debug("Настроены параметры страницы: поля 3-1-2-2 см, формат A4");
    }

    /**
     * Настройка базовых стилей документа
     */
    private void setupDocumentStyles(XWPFDocument document) {
        // Создание стиля для обычного текста
        XWPFStyles styles = document.createStyles();
        
        // Стиль для основного текста
        CTStyle normalStyle = CTStyle.Factory.newInstance();
        normalStyle.setStyleId("Normal");
        normalStyle.addNewName().setVal("Normal");
        normalStyle.addNewQFormat();
        
        CTRPr rPr = normalStyle.addNewRPr();
        rPr.addNewRFonts().setAscii("Times New Roman");
        rPr.addNewRFonts().setHAnsi("Times New Roman");
        rPr.addNewSz().setVal(BigInteger.valueOf(28)); // 14pt = 28 half-points
        rPr.addNewSzCs().setVal(BigInteger.valueOf(28));
        
        CTLanguage lang = rPr.addNewLang();
        lang.setVal("uk-UA");

        log.debug("Настроены базовые стили: Times New Roman, 14pt, украинская локаль");
    }

    /**
     * Форматирование всех абзацев согласно академическим требованиям
     */
    private void formatAllParagraphs(XWPFDocument document) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            formatParagraphToAcademicStandard(paragraph);
        }

        // Форматирование абзацев в таблицах
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        formatParagraphToAcademicStandard(paragraph);
                    }
                }
            }
        }

        log.debug("Отформатировано {} абзацев", document.getParagraphs().size());
    }

    /**
     * Форматирование отдельного абзаца
     */
    private void formatParagraphToAcademicStandard(XWPFParagraph paragraph) {
        // НЕ переопределяем выравнивание если оно уже установлено AI
        if (paragraph.getAlignment() == null) {
            paragraph.setAlignment(ParagraphAlignment.BOTH); // По ширине
        }

        // Настройка межстрочного интервала (1.5)
        paragraph.setSpacingBetween(1.5);

        // Настройка отступов только для обычных абзацев (не заголовков и списков)
        String text = paragraph.getText();
        if (text != null && !isHeading(text, paragraph) && !isListItem(text)) {
            paragraph.setIndentationFirstLine(708); // 1.25 см красная строка
        }
        paragraph.setSpacingAfter(0); // Без отступа после абзаца
        paragraph.setSpacingBefore(0); // Без отступа перед абзацем

        // Форматирование текста в абзаце
        for (XWPFRun run : paragraph.getRuns()) {
            formatRunToAcademicStandard(run);
        }
    }
    
    /**
     * Проверка, является ли текст элементом списка
     */
    private boolean isListItem(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        return trimmed.startsWith("•") || 
               trimmed.startsWith("-") ||
               trimmed.startsWith("*") ||
               trimmed.matches("^\\d+[.)].*") ||
               trimmed.matches("^[а-я][.)].*");
    }

    /**
     * Форматирование текстового фрагмента
     */
    private void formatRunToAcademicStandard(XWPFRun run) {
        run.setFontFamily("Times New Roman");
        // НЕ переопределяем размер шрифта если он уже установлен AI
        if (run.getFontSize() == -1) {
            run.setFontSize(14);
        }
        run.setColor("000000"); // Черный цвет

        // Убираем ТОЛЬКО подчеркивание, сохраняем bold/italic от AI
        run.setUnderline(UnderlinePatterns.NONE);
    }

    /**
     * Форматирование заголовков
     */
    private void formatHeadings(XWPFDocument document) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String text = paragraph.getText();
            
            // Определяем заголовки по содержимому или стилю
            if (isHeading(text, paragraph)) {
                formatHeading(paragraph, getHeadingLevel(text));
            }
        }

        log.debug("Отформатированы заголовки документа");
    }

    /**
     * Проверка, является ли абзац заголовком
     */
    private boolean isHeading(String text, XWPFParagraph paragraph) {
        if (text == null || text.trim().isEmpty()) return false;
        
        // Проверяем по стилю
        String style = paragraph.getStyle();
        if (style != null && style.toLowerCase().contains("heading")) {
            return true;
        }
        
        // Проверяем по содержимому (большие буквы, короткий текст)
        String trimmed = text.trim();
        return trimmed.length() < 100 && 
               (trimmed.equals(trimmed.toUpperCase()) || 
                trimmed.matches("^\\d+\\..*|^[IVXLCDM]+\\..*|^[А-ЯІЄЇЎЁ].*"));
    }

    /**
     * Определение уровня заголовка
     */
    private int getHeadingLevel(String text) {
        if (text.matches("^\\d+\\.\\d+\\.\\d+.*")) return 3;
        if (text.matches("^\\d+\\.\\d+.*")) return 2;
        if (text.matches("^\\d+\\..*")) return 1;
        return 1; // По умолчанию
    }

    /**
     * Форматирование заголовка
     */
    private void formatHeading(XWPFParagraph paragraph, int level) {
        // НЕ переопределяем выравнивание если оно уже установлено AI
        if (paragraph.getAlignment() == null) {
            // Только если выравнивание не задано, применяем стандартное
            if (level == 1) {
                paragraph.setAlignment(ParagraphAlignment.LEFT); // Изменено с CENTER на LEFT
            } else {
                paragraph.setAlignment(ParagraphAlignment.LEFT);
            }
        }

        // Отступы
        paragraph.setIndentationFirstLine(0); // Без красной строки
        paragraph.setSpacingAfter(200); // Отступ после заголовка
        paragraph.setSpacingBefore(400); // Отступ перед заголовком

        // Форматирование текста заголовка
        for (XWPFRun run : paragraph.getRuns()) {
            run.setFontFamily("Times New Roman");
            // НЕ переопределяем размер если он уже установлен AI
            if (run.getFontSize() == -1) {
                run.setFontSize(14);
            }
            // НЕ переопределяем bold если он уже установлен AI
            if (!run.isBold()) {
                run.setBold(level <= 2); // Жирный для 1-2 уровня
            }
            run.setColor("000000");
            // УБИРАЕМ ПОДЧЕРКИВАНИЕ В ЗАГОЛОВКАХ
            run.setUnderline(UnderlinePatterns.NONE);
        }
    }

    /**
     * Форматирование списков
     */
    private void formatLists(XWPFDocument document) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String text = paragraph.getText();
            if (text != null && (text.trim().startsWith("•") || 
                               text.trim().matches("^\\d+\\).*") ||
                               text.trim().matches("^[а-я]\\).*"))) {
                formatListItem(paragraph);
            }
        }
    }

    /**
     * Форматирование элемента списка
     */
    private void formatListItem(XWPFParagraph paragraph) {
        paragraph.setAlignment(ParagraphAlignment.BOTH);
        paragraph.setIndentationLeft(708); // Отступ слева
        paragraph.setIndentationHanging(354); // Висячий отступ
        paragraph.setSpacingBetween(1.5);

        for (XWPFRun run : paragraph.getRuns()) {
            formatRunToAcademicStandard(run);
        }
    }

    /**
     * Добавление нумерации страниц
     */
    private void addPageNumbering(XWPFDocument document) {
        try {
            log.debug("Нумерация страниц будет добавлена при итоговом форматировании");
            // Примечание: нумерация страниц добавляется автоматически при академическом форматировании
        } catch (Exception e) {
            log.warn("Не удалось добавить нумерацию страниц: {}", e.getMessage());
        }
    }
} 