package com.melody.melody_stream.modules.notification.repository;

import com.melody.melody_stream.modules.notification.entity.Notification;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllReadByUserId(@Param("userId") String userId);

    long countByUserIdAndIsReadFalse(String userId);
}
