package com.melody.melody_stream.modules.auth.repository;

import com.melody.melody_stream.modules.auth.entity.RefreshToken;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Latest active token for a given user + device
    Optional<RefreshToken> findByUserIdAndDeviceIdAndIsRevokedFalse(String userId, String deviceId);

    // All active tokens for a user (logout all devices)
    List<RefreshToken> findByUserIdAndIsRevokedFalse(String userId);

    // Bulk revoke - for reuse attack or logout all devices
    @Modifying
    @Query("""
        UPDATE RefreshToken rt SET
            rt.isRevoked = true,
            rt.revokedAt = :now,
            rt.revokedBy = :by
        WHERE rt.userId = :userId AND rt.isRevoked = false
    """)
    void revokeAllByUserId(
            @Param("userId") String userId,
            @Param("now")LocalDateTime now,
            @Param("by") String by
    );

    // Cleanup job - delete expired tokens older than X days
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoff")
    void deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
