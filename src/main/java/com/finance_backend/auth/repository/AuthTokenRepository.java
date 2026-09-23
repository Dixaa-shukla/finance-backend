package com.finance_backend.auth.repository;

import com.finance_backend.auth.entity.AuthToken;
import com.finance_backend.auth.entity.AuthTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByTokenAndTokenType(String token, AuthTokenType tokenType);

    @Modifying
    @Query("UPDATE AuthToken t SET t.usedAt = :usedAt "
            + "WHERE t.userId = :userId AND t.tokenType = :tokenType AND t.usedAt IS NULL")
    int invalidateOutstanding(@Param("userId") Long userId,
                              @Param("tokenType") AuthTokenType tokenType,
                              @Param("usedAt") LocalDateTime usedAt);

    /**
     * @param cutoff delete anything that expired before this instant
     * @return number of rows deleted
     */
    @Modifying
    @Query("DELETE FROM AuthToken t WHERE t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
