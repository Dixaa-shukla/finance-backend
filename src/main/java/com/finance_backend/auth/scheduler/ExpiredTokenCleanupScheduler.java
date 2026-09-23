package com.finance_backend.auth.scheduler;

import com.finance_backend.auth.repository.AuthTokenRepository;
import com.finance_backend.auth.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
public class ExpiredTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthTokenRepository authTokenRepository;

    public ExpiredTokenCleanupScheduler(RefreshTokenRepository refreshTokenRepository,
                                        AuthTokenRepository authTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.authTokenRepository = authTokenRepository;
    }

    /**
     * Runs daily at 03:15 -- deliberately outside the 08:00 slot the notification
     * and goal schedulers share, so a slow delete never delays a user-facing job.
     */
    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();

        int refreshDeleted = refreshTokenRepository.deleteExpiredBefore(now);
        int authDeleted = authTokenRepository.deleteExpiredBefore(now);

        if (refreshDeleted > 0 || authDeleted > 0) {
            log.info("Token cleanup removed {} expired refresh token(s) and {} expired auth token(s)",
                    refreshDeleted, authDeleted);
        } else {
            log.debug("Token cleanup found nothing to remove");
        }
    }
}
