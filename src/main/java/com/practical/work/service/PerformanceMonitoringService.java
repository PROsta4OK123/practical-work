package com.practical.work.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class PerformanceMonitoringService {

    private final ConcurrentHashMap<String, Long> apiCallCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> apiResponseTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    public void recordApiCall(String endpoint, long responseTime) {
        apiCallCounts.merge(endpoint, 1L, Long::sum);
        apiResponseTimes.computeIfAbsent(endpoint, k -> new AtomicLong(0))
                        .addAndGet(responseTime);
    }

    public void recordError(String endpoint) {
        errorCounts.computeIfAbsent(endpoint, k -> new AtomicLong(0))
                   .incrementAndGet();
    }

    public void recordUserAction(String action, String userId) {
        log.info("USER_ACTION: {} by user {}", action, userId);
    }

    public void recordSecurityEvent(String event, String details) {
        log.warn("SECURITY_EVENT: {} - {}", event, details);
    }

    @Scheduled(fixedRate = 60000) // Каждую минуту
    public void logSystemMetrics() {
        try {
            MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
            
            long uptime = runtimeBean.getUptime();
            int availableProcessors = Runtime.getRuntime().availableProcessors();
            int activeThreads = threadBean.getThreadCount();
            
            double heapUsedPercent = (double) heapMemory.getUsed() / heapMemory.getMax() * 100;
            
            log.info("SYSTEM_METRICS - " +
                    "Heap: {}MB/{} MB ({}%), " +
                    "NonHeap: {}MB/{} MB, " +
                    "Threads: {}, " +
                    "Processors: {}, " +
                    "Uptime: {}ms",
                    heapMemory.getUsed() / 1024 / 1024,
                    heapMemory.getMax() / 1024 / 1024,
                    String.format("%.1f", heapUsedPercent),
                    nonHeapMemory.getUsed() / 1024 / 1024,
                    nonHeapMemory.getMax() > 0 ? nonHeapMemory.getMax() / 1024 / 1024 : "unlimited",
                    activeThreads,
                    availableProcessors,
                    uptime);
            
            // Предупреждение при высоком использовании памяти
            if (heapUsedPercent > 80) {
                log.warn("HIGH_MEMORY_USAGE: {}% heap memory used", String.format("%.1f", heapUsedPercent));
            }
            
        } catch (Exception e) {
            log.error("Ошибка сбора метрик системы", e);
        }
    }

    @Scheduled(fixedRate = 300000) // Каждые 5 минут
    public void logApiStatistics() {
        if (apiCallCounts.isEmpty()) {
            return;
        }

        log.info("API_STATISTICS:");
        
        apiCallCounts.forEach((endpoint, count) -> {
            AtomicLong totalTime = apiResponseTimes.get(endpoint);
            AtomicLong errors = errorCounts.get(endpoint);
            
            long avgResponseTime = totalTime != null ? totalTime.get() / count : 0;
            long errorCount = errors != null ? errors.get() : 0;
            double errorRate = count > 0 ? (double) errorCount / count * 100 : 0;
            
            log.info("  {} - Calls: {}, Avg Response: {}ms, Errors: {} ({}%)",
                    endpoint, count, avgResponseTime, errorCount, 
                    String.format("%.1f", errorRate));
        });
        
        // Очищаем статистику после логгирования
        clearStatistics();
    }

    @Scheduled(fixedRate = 86400000) // Каждые 24 часа
    public void logDailyReport() {
        try {
            long totalUptime = runtimeBean.getUptime();
            MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
            
            log.info("DAILY_REPORT - " +
                    "Total uptime: {} hours, " +
                    "Peak memory: {}MB, " +
                    "Current memory: {}MB",
                    totalUptime / 1000 / 3600,
                    heapMemory.getMax() / 1024 / 1024,
                    heapMemory.getUsed() / 1024 / 1024);
                    
        } catch (Exception e) {
            log.error("Ошибка формирования ежедневного отчета", e);
        }
    }

    public void logConnectionError(String endpoint, String error) {
        log.error("CONNECTION_ERROR: {} - {}", endpoint, error);
        recordError(endpoint);
    }

    public void logSlowQuery(String query, long duration) {
        if (duration > 1000) {
            log.warn("SLOW_QUERY: {} took {}ms", query, duration);
        }
    }

    public void logFileOperation(String operation, String fileName, long fileSize, long duration) {
        log.info("FILE_OPERATION: {} - {} ({} bytes) in {}ms", 
                operation, fileName, fileSize, duration);
    }

    private void clearStatistics() {
        apiCallCounts.clear();
        apiResponseTimes.clear();
        errorCounts.clear();
    }

    public String getSystemStatus() {
        try {
            MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
            double heapUsedPercent = (double) heapMemory.getUsed() / heapMemory.getMax() * 100;
            int activeThreads = threadBean.getThreadCount();
            
            return String.format("Heap: %.1f%%, Threads: %d", heapUsedPercent, activeThreads);
        } catch (Exception e) {
            return "Status unavailable";
        }
    }
} 