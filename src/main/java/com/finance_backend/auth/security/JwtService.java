package com.finance_backend.auth.security;

import com.finance_backend.auth.entity.Role;
import com.finance_backend.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JwtService {

    /**
     * HS256 needs a 256-bit key.
     */
    private static final int MIN_SECRET_BYTES = 32;

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLES = "roles";

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtService(
            @Value("${spring.security.jwt.secret:}") String secret,
            @Value("${spring.security.jwt.issuer:auth-backend}") String issuer,
            @Value("${spring.security.jwt.access-ttl-seconds:900}") long accessTtlSeconds) {

        String trimmed = secret == null ? "" : secret.trim();
        byte[] keyBytes = trimmed.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "spring.security.jwt.secret is missing or too short (" + keyBytes.length + " bytes). "
                            + "It must be at least " + MIN_SECRET_BYTES + " characters. "
                            + "Set the JWT_SECRET environment variable -- generate one with: openssl rand -base64 48");
        }

        /* secret is at least 256 bits and key length determine the HMAC algorithm used for the JWT. */
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        this.accessTokenTtl = Duration.ofSeconds(accessTtlSeconds);

        log.info("JwtService ready -- issuer='{}', access token TTL={} seconds", issuer, accessTtlSeconds);
    }

    // ==================== ISSUING ====================

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenTtl);

        List<String> roles = user.getRoles() == null
                ? List.of(Role.USER.name())
                : user.getRoles().stream().map(Role::name).sorted().toList();

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuer(issuer)
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLES, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /** Access-token lifetime in seconds, for AuthResponse.expiresIn. */
    public long getAccessTokenSeconds() {
        return accessTokenTtl.toSeconds();
    }

    // ==================== VALIDATING ====================

    public Optional<AuthPrincipal> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = Long.valueOf(claims.getSubject());
            String email = claims.get(CLAIM_EMAIL, String.class);
            Set<String> roles = extractRoles(claims);

            return Optional.of(new AuthPrincipal(userId, email, roles));

        } catch (JwtException | IllegalArgumentException ex) {
            // Expired, tampered, wrong issuer, malformed, or a non-numeric subject.
            log.debug("Rejected JWT: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * if no roles are found, it gives the user default USER role.
     */
    private Set<String> extractRoles(Claims claims) {
        Object raw = claims.get(CLAIM_ROLES);

        if (raw instanceof List<?> list && !list.isEmpty()) {
            return list.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return Set.of(Role.USER.name());
    }
}
