package com.melody.melody_stream.modules.notification.entity;

import com.melody.melody_stream.core.entity.AuditableEntity;
import com.melody.melody_stream.core.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Notification extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = true)
    private String message;

    @Column(name = "reference_id", nullable = true)
    private String referenceId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

}