package com.melody.melody_stream.modules.auth.job;

import com.melody.melody_stream.modules.auth.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Runs daily at 3 AM — delete refresh tokens expired more than 30 days ago.
     * Keeps recent revoked tokens for audit trail.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        refreshTokenRepository.deleteExpiredBefore(cutoff);
        log.info("Cleaned up expired refresh tokens older than {}", cutoff);
    }

}
