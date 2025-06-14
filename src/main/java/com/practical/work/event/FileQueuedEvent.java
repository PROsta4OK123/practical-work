package com.practical.work.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class FileQueuedEvent extends ApplicationEvent {
    private final String fileId;
    private final Long queueId;

    public FileQueuedEvent(Object source, String fileId, Long queueId) {
        super(source);
        this.fileId = fileId;
        this.queueId = queueId;
    }
} 