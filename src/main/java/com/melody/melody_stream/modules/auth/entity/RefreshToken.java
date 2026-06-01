package com.melody.melody_stream.modules.auth.entity;

import com.melody.melody_stream.core.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// RefreshToken does not include deletedAt/deletedBy
// Not need to extend AuditableEntity, only 4 basic audit fields
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_rt_token_hash", columnList = "token_hash"),
        @Index(name = "idx_rt_user_device", columnList = "user_id, device_id"),
        @Index(name = "idx_rt_is_revoked", columnList = "is_revoked"),
        @Index(name = "idx_rt_expires_at", columnList = "expires_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "token_hash", unique = true, nullable = false)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked = false;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private String revokedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    // ── Helpers ───────────────────────────────────────────────
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isValid() {
        return !this.isRevoked && !this.isExpired();
    }

    public void revoke(String revokedBy) {
        this.isRevoked  = true;
        this.revokedAt  = LocalDateTime.now();
        this.revokedBy  = revokedBy;
    }
}