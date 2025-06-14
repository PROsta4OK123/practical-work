package com.practical.work.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Создаем wrappers для кеширования содержимого
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        String requestId = generateRequestId();
        long startTime = System.currentTimeMillis();
        
        // Логируем входящий запрос
        logRequest(requestWrapper, requestId);
        
        try {
            // Выполняем запрос
            filterChain.doFilter(requestWrapper, responseWrapper);
            
        } catch (Exception e) {
            log.error("[{}] Ошибка обработки запроса: {}", requestId, e.getMessage(), e);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // Логируем ответ
            logResponse(responseWrapper, requestId, duration);
            
            // Важно: копируем содержимое обратно в ответ
            responseWrapper.copyBodyToResponse();
        }
    }
    
    private void logRequest(HttpServletRequest request, String requestId) {
        log.info("[{}] --> {} {} (IP: {}, User-Agent: {})", 
                requestId,
                request.getMethod(), 
                request.getRequestURI(),
                getClientIP(request),
                request.getHeader("User-Agent"));
        
        // Логируем заголовки (без чувствительной информации)
        logHeaders(request, requestId);
        
        // Логируем параметры запроса
        if (!request.getParameterMap().isEmpty()) {
            log.debug("[{}] Параметры запроса: {}", requestId, request.getParameterMap());
        }
    }
    
    private void logResponse(ContentCachingResponseWrapper response, String requestId, long duration) {
        int status = response.getStatus();
        String statusCategory = getStatusCategory(status);
        
        log.info("[{}] <-- {} {} ({}ms)", requestId, status, statusCategory, duration);
        
        // Логируем предупреждение для медленных запросов
        if (duration > 1000) {
            log.warn("[{}] МЕДЛЕННЫЙ ЗАПРОС: {}ms", requestId, duration);
        }
        
        // Логируем ошибки с содержимым ответа
        if (status >= 400) {
            byte[] content = response.getContentAsByteArray();
            if (content.length > 0) {
                try {
                    String characterEncoding = response.getCharacterEncoding();
                    Charset charset = (characterEncoding != null && Charset.isSupported(characterEncoding)) 
                                        ? Charset.forName(characterEncoding) 
                                        : StandardCharsets.UTF_8;
                    String responseBody = new String(content, charset);
                    log.error("[{}] Ошибка ответа: {}", requestId, responseBody);
                } catch (Exception e) {
                    log.error("[{}] Не удалось прочитать тело ответа: {}", requestId, new String(content));
                }
            }
        }
    }
    
    private void logHeaders(HttpServletRequest request, String requestId) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            
            // Скрываем чувствительные заголовки
            if (isSensitiveHeader(headerName)) {
                log.debug("[{}] Header {}: [СКРЫТО]", requestId, headerName);
            } else {
                String headerValue = request.getHeader(headerName);
                log.debug("[{}] Header {}: {}", requestId, headerName, headerValue);
            }
        }
    }
    
    private boolean isSensitiveHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return lowerName.equals("authorization") || 
               lowerName.equals("cookie") || 
               lowerName.equals("x-api-key") ||
               lowerName.contains("token");
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
    
    private String getStatusCategory(int status) {
        if (status >= 200 && status < 300) return "SUCCESS";
        if (status >= 300 && status < 400) return "REDIRECT";
        if (status >= 400 && status < 500) return "CLIENT_ERROR";
        if (status >= 500) return "SERVER_ERROR";
        return "INFO";
    }
    
    private String generateRequestId() {
        return "REQ-" + System.currentTimeMillis() + "-" + 
               (int)(Math.random() * 1000);
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String uri = request.getRequestURI();
        
        // Не логируем healthcheck и статические ресурсы
        return uri.contains("/actuator/") || 
               uri.contains("/health") ||
               uri.contains("/static/") ||
               uri.contains("/webjars/");
    }
} 