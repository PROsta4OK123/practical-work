package com.practical.work.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Событие обновления количества чанков в документе
 * Используется для уведомления о том, что документ был проанализирован и разбит на чанки
 */
@Getter
public class DocumentChunksCountUpdatedEvent extends ApplicationEvent {
    
    private final String fileId;
    private final int chunksCount;
    
    public DocumentChunksCountUpdatedEvent(Object source, String fileId, int chunksCount) {
        super(source);
        this.fileId = fileId;
        this.chunksCount = chunksCount;
    }
} 