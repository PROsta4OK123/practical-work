package com.practical.work.repository;

import com.practical.work.model.ProcessedDocument;
import com.practical.work.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessedDocumentRepository extends JpaRepository<ProcessedDocument, Long> {
    
    Optional<ProcessedDocument> findByFileId(String fileId);
    
    List<ProcessedDocument> findByUserOrderByCreatedAtDesc(User user);
    
    List<ProcessedDocument> findByStatus(ProcessedDocument.ProcessingStatus status);
} 