package com.finance_backend.auth.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Component
public class RefreshTokenCookieService {

    private static final String COOKIE_PATH = "/api/v1/auth";

    private final String cookieName;
    private final boolean secure;
    private final String sameSite;
    private final Duration maxAge;

    public RefreshTokenCookieService(
            @Value("${spring.security.jwt.refresh-cookie-name:refresh_token}") String cookieName,
            @Value("${spring.security.jwt.cookie-secure:false}") boolean secure,
            @Value("${spring.security.jwt.cookie-same-site:Lax}") String sameSite,
            @Value("${spring.security.jwt.refresh-ttl-seconds:1209600}") long refreshTtlSeconds) {

        this.cookieName = cookieName;
        this.secure = secure;
        this.sameSite = sameSite;
        this.maxAge = Duration.ofSeconds(refreshTtlSeconds);

        if (!secure) {
            log.info("Refresh cookie '{}' is NOT marked Secure -- correct for http://localhost, "
                    + "but set spring.security.jwt.cookie-secure=true before deploying over HTTPS.", cookieName);
        }
    }

    // ==================== WRITING ====================

    public void write(HttpServletResponse response, String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        ResponseCookie cookie = ResponseCookie.from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // ==================== READING ====================

    /** The cookie's value, if the request carries one. */
    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

    public Optional<String> resolve(HttpServletRequest request, String bodyToken) {
        if (bodyToken != null && !bodyToken.isBlank()) {
            return Optional.of(bodyToken);
        }
        return read(request);
    }

    public String getCookieName() {
        return cookieName;
    }
}
