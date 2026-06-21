package com.melody.melody_stream.modules.notification.service;

import com.melody.melody_stream.core.enums.JobStatus;
import com.melody.melody_stream.core.enums.NotificationType;
import com.melody.melody_stream.modules.job.entity.Job;
import com.melody.melody_stream.modules.job.repository.JobRepository;
import com.melody.melody_stream.modules.notification.dto.NotificationResponse;
import com.melody.melody_stream.modules.notification.entity.Notification;
import com.melody.melody_stream.modules.notification.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Create noti in DB + push websocket
    public void send(String userId, NotificationType type, String title, String message, String referenceId) {
        // 1. Save to DB
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .isRead(false)
                .build();
        notification = notificationRepository.save(notification);

        // 2. Push WebSocket if user is online
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId,
                NotificationResponse.from(notification)
        );
    }

    // Get unread notification lists
    public List<NotificationResponse> getUnread(String userId) {
        return notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public void markAllRead(String userId) {
        notificationRepository.markAllReadByUserId(userId);
    }
}
