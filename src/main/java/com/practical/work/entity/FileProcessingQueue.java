package com.practical.work.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import com.practical.work.model.User;

@Entity
@Table(name = "file_processing_queue")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileProcessingQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_id", nullable = false, unique = true)
    private String fileId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QueueStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private Priority priority;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "estimated_threads")
    private Integer estimatedThreads;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "error_message")
    private String errorMessage;

    public enum QueueStatus {
        PENDING,     // В очереди, ожидает обработки
        PROCESSING,  // Обрабатывается
        COMPLETED,   // Обработка завершена успешно
        FAILED,      // Обработка завершена с ошибкой
        CANCELLED    // Отменена
    }

    public enum Priority {
        LOW,         // Большие файлы, низкий приоритет
        NORMAL,      // Обычные файлы
        HIGH         // Маленькие файлы, высокий приоритет
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = QueueStatus.PENDING;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }
} 