package com.practical.work.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Консервативные настройки для ограничения ресурсов
        executor.setCorePoolSize(2); // Базовое количество потоков
        executor.setMaxPoolSize(6); // Максимум 6 потоков (2 документа по 3 потока)
        executor.setQueueCapacity(50); // Ограниченная очередь
        executor.setThreadNamePrefix("DocumentProcessor-");
        executor.setKeepAliveSeconds(30); // Быстрое освобождение неиспользуемых потоков
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        // Политика отклонения при переполнении - заблокировать до освобождения места
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
} 