package com.practical.work.repository;

import com.practical.work.entity.FileProcessingQueue;
import com.practical.work.entity.FileProcessingQueue.QueueStatus;
import com.practical.work.entity.FileProcessingQueue.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileProcessingQueueRepository extends JpaRepository<FileProcessingQueue, Long> {

    // Найти файл в очереди по fileId
    Optional<FileProcessingQueue> findByFileId(String fileId);

    // Получить все файлы в определенном статусе
    List<FileProcessingQueue> findByStatusOrderByCreatedAtAsc(QueueStatus status);

    // Получить следующий файл для обработки с учетом приоритета
    @Query("SELECT q FROM FileProcessingQueue q WHERE q.status = :status " +
           "ORDER BY q.priority DESC, q.createdAt ASC")
    List<FileProcessingQueue> findNextForProcessing(@Param("status") QueueStatus status);

    // Получить количество файлов в очереди по статусу
    long countByStatus(QueueStatus status);

    // Получить количество активных обработок
    @Query("SELECT COUNT(q) FROM FileProcessingQueue q WHERE q.status = :status")
    long countActiveProcessing(@Param("status") QueueStatus status);

    // Получить файлы пользователя в очереди
    List<FileProcessingQueue> findByUserIdAndStatusInOrderByCreatedAtDesc(
        Long userId, List<QueueStatus> statuses);

    // Получить файлы, которые обрабатываются дольше определенного времени (для мониторинга зависших)
    @Query("SELECT q FROM FileProcessingQueue q WHERE q.status = :status " +
           "AND q.startedAt < :timeThreshold")
    List<FileProcessingQueue> findStuckProcessing(
        @Param("status") QueueStatus status, 
        @Param("timeThreshold") LocalDateTime timeThreshold);

    // Очистить старые завершенные/ошибочные записи
    @Modifying
    @Query("DELETE FROM FileProcessingQueue q WHERE q.status IN :statuses " +
           "AND q.createdAt < :cutoffDate")
    void deleteOldRecords(
        @Param("statuses") List<QueueStatus> statuses, 
        @Param("cutoffDate") LocalDateTime cutoffDate);

    // Получить статистику очереди
    @Query("SELECT q.status, COUNT(q) FROM FileProcessingQueue q GROUP BY q.status")
    List<Object[]> getQueueStatistics();

    /**
     * Получает группированную статистику очереди одним запросом
     */
    @Query("SELECT fpq.status, COUNT(fpq) FROM FileProcessingQueue fpq GROUP BY fpq.status")
    List<Object[]> getQueueStatisticsGrouped();
} 