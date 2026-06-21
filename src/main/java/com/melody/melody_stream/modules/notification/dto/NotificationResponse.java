package com.melody.melody_stream.modules.notification.dto;

import com.melody.melody_stream.core.enums.NotificationType;
import com.melody.melody_stream.modules.notification.entity.Notification;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {
    private String id;
    private String userId;
    private NotificationType type;
    private String title;
    private String message;
    private String referenceId;
    private boolean isRead;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceId(n.getReferenceId())
                .isRead(n.isRead())
                .build();
    }
}