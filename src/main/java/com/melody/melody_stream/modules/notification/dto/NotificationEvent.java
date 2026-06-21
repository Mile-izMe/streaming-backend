package com.melody.melody_stream.modules.notification.dto;

import com.melody.melody_stream.core.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationEvent {
    private String userId;
    private NotificationType type;
    private String title;
    private String message;
    private String referenceId;
}