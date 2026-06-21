package com.melody.melody_stream.modules.notification.controller;

import com.melody.melody_stream.modules.auth.dto.response.JwtPayload;
import com.melody.melody_stream.modules.notification.dto.NotificationResponse;
import com.melody.melody_stream.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getUnread(
            @AuthenticationPrincipal JwtPayload principal
    ) {
        return ResponseEntity.ok(notificationService.getUnread(principal.sub()));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal JwtPayload principal
    ) {
        notificationService.markAllRead(principal.sub());
        return ResponseEntity.noContent().build();
    }
}
