package com.finance_backend.auth.security;

import com.finance_backend.auth.dto.AuthResponse;
import com.finance_backend.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String ATTR_EMAIL = "email";
    private static final String ATTR_SUB = "sub";
    private static final String ATTR_EMAIL_VERIFIED = "email_verified";

    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final String frontendRedirectUri;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public OAuth2LoginSuccessHandler(
            AuthService authService,
            RefreshTokenCookieService refreshTokenCookieService,
            @Value("${finance.auth.frontend-redirect-uri:http://localhost:5173/oauth2/callback}")
            String frontendRedirectUri) {

        this.authService = authService;
        this.refreshTokenCookieService = refreshTokenCookieService;
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        if (!(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) {
            log.warn("OAuth2 success handler invoked with an unexpected principal type: {}",
                    authentication.getPrincipal() == null
                            ? "null"
                            : authentication.getPrincipal().getClass().getName());
            redirectWithError(request, response, "oauth2_unexpected_principal");
            return;
        }

        String email = asString(oAuth2User.getAttribute(ATTR_EMAIL));
        String providerId = asString(oAuth2User.getAttribute(ATTR_SUB));
        boolean emailVerified = asBoolean(oAuth2User.getAttribute(ATTR_EMAIL_VERIFIED));

        if (email == null || email.isBlank() || providerId == null || providerId.isBlank()) {
            log.warn("Google returned no usable email/sub -- check that "
                    + "spring.security.oauth2.client.registration.google.scope still includes 'email'");
            redirectWithError(request, response, "oauth2_missing_email");
            return;
        }

        AuthResponse tokens;
        try {
            tokens = authService.loginWithGoogle(email, providerId, emailVerified);

        } catch (RuntimeException e) {
            // Disabled account, or a refused link because Google reports the
            // address as unverified. Never leak the reason into a URL.
            log.warn("Google sign-in rejected for {}: {}", email, e.getMessage());
            redirectWithError(request, response, "oauth2_login_rejected");
            return;
        }

        finishStatelessly(request);

        refreshTokenCookieService.write(response, tokens.getRefreshToken());

        String target = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("accessToken", tokens.getAccessToken())
                .queryParam("refreshToken", tokens.getRefreshToken())
                .queryParam("tokenType", tokens.getTokenType())
                .queryParam("expiresIn", tokens.getExpiresIn())
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        log.info("Google sign-in complete for userId={}, redirecting to {}",
                tokens.getUserId(), frontendRedirectUri);

        redirectStrategy.sendRedirect(request, response, target);
    }

    // ==================== INTERNAL ====================

    private void finishStatelessly(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    private void redirectWithError(HttpServletRequest request,
                                  HttpServletResponse response,
                                  String errorCode) throws IOException {

        finishStatelessly(request);

        String target = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("error", errorCode)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        redirectStrategy.sendRedirect(request, response, target);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }
}
