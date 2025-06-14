package com.practical.work.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class LoggingExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception e, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        
        log.error("[{}] Необработанная ошибка в {} {}: {}", 
                requestId, request.getMethod(), request.getRequestURI(), e.getMessage(), e);
        
        Map<String, Object> response = createErrorResponse(
                "Внутренняя ошибка сервера", 
                e.getMessage(),
                requestId
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<Map<String, Object>> handleAsyncRequestNotUsable(
            AsyncRequestNotUsableException e, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        
        log.warn("[{}] Клиент закрыл соединение для {} {}: {}", 
                requestId, request.getMethod(), request.getRequestURI(), e.getMessage());
        
        // Возвращаем минимальный ответ, так как клиент уже отключился
        Map<String, Object> response = createErrorResponse(
                "Соединение прервано клиентом", 
                "Client disconnected",
                requestId
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException e, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        
        log.warn("[{}] Ошибка аутентификации в {} {}: {}", 
                requestId, request.getMethod(), request.getRequestURI(), e.getMessage());
        
        Map<String, Object> response = createErrorResponse(
                "Ошибка аутентификации", 
                e.getMessage(),
                requestId
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException e, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        
        log.warn("[{}] Отказ в доступе для {} {}: {}", 
                requestId, request.getMethod(), request.getRequestURI(), e.getMessage());
        
        Map<String, Object> response = createErrorResponse(
                "Доступ запрещен", 
                e.getMessage(),
                requestId
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        
        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        log.warn("[{}] Ошибка валидации для {} {}: {}", 
                requestId, request.getMethod(), request.getRequestURI(), fieldErrors);
        
        Map<String, Object> response = createErrorResponse(
                "Ошибка валидации данных", 
                "Проверьте правильность заполнения полей",
                requestId
        );
        response.put("fieldErrors", fieldErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException e, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        
        log.warn("[{}] Превышен размер файла для {} {}: {}", 
                requestId, request.getMethod(), request.getRequestURI(), e.getMessage());
        
        Map<String, Object> response = createErrorResponse(
                "Файл слишком большой", 
                "Максимальный размер файла: 50MB",
                requestId
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException e, HttpServletRequest request) {
        
        String requestId = generateRequestId();
        
        // Определяем уровень серьезности ошибки
        if (isBusinessLogicException(e)) {
            log.warn("[{}] Бизнес-ошибка в {} {}: {}", 
                    requestId, request.getMethod(), request.getRequestURI(), e.getMessage());
            
            Map<String, Object> response = createErrorResponse(
                    "Ошибка выполнения операции", 
                    e.getMessage(),
                    requestId
            );
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } else {
            log.error("[{}] Системная ошибка в {} {}: {}", 
                    requestId, request.getMethod(), request.getRequestURI(), e.getMessage(), e);
            
            Map<String, Object> response = createErrorResponse(
                    "Внутренняя ошибка сервера", 
                    e.getMessage(),
                    requestId
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private boolean isBusinessLogicException(RuntimeException e) {
        String message = e.getMessage();
        return message != null && (
                message.contains("не найден") ||
                message.contains("уже существует") ||
                message.contains("недостаточно") ||
                message.contains("неверный") ||
                message.contains("заблокирован") ||
                message.contains("некорректный")
        );
    }

    private Map<String, Object> createErrorResponse(String error, String details, String requestId) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", error);
        response.put("details", details);
        response.put("requestId", requestId);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    private String generateRequestId() {
        return "ERR-" + System.currentTimeMillis() + "-" + 
               (int)(Math.random() * 1000);
    }
} 