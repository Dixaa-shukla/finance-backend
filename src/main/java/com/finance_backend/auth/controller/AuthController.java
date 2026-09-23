package com.finance_backend.auth.controller;

import com.finance_backend.auth.dto.AuthResponse;
import com.finance_backend.auth.dto.ChangePasswordRequest;
import com.finance_backend.auth.dto.EmailRequest;
import com.finance_backend.auth.dto.LoginRequest;
import com.finance_backend.auth.dto.MessageResponse;
import com.finance_backend.auth.dto.RefreshTokenRequest;
import com.finance_backend.auth.dto.RegisterRequest;
import com.finance_backend.auth.dto.ResetPasswordRequest;
import com.finance_backend.auth.dto.UserResponse;
import com.finance_backend.auth.security.RefreshTokenCookieService;
import com.finance_backend.auth.service.AuthService;
import com.finance_backend.common.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    // Constructor Injection
    public AuthController(AuthService authService,
                          RefreshTokenCookieService refreshTokenCookieService) {
        this.authService = authService;
        this.refreshTokenCookieService = refreshTokenCookieService;
    }

    // ==================== REGISTRATION & LOGIN ====================

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.login(request);

        refreshTokenCookieService.write(response, authResponse.getRefreshToken());

        return ResponseEntity.ok(authResponse);
    }

    // ==================== SESSION ====================

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String refreshToken = resolveRefreshToken(request, httpRequest)
                .orElseThrow(() -> new BadRequestException(
                        "refreshToken is required -- send it in the request body or in the "
                                + refreshTokenCookieService.getCookieName() + " cookie"));

        AuthResponse authResponse = authService.refresh(refreshToken);

        refreshTokenCookieService.write(httpResponse, authResponse.getRefreshToken());

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        resolveRefreshToken(request, httpRequest).ifPresent(authService::logout);

        refreshTokenCookieService.clear(httpResponse);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(HttpServletResponse response) {

        authService.logoutAll();

        refreshTokenCookieService.clear(response);

        return ResponseEntity.noContent().build();
    }

    /**
     * NOTE:
     * Reads from the database rather than echoing the token's claims, so a role
     * change or a disabled account shows up here immediately instead of after
     * the token expires.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {

        return ResponseEntity.ok(
                authService.getCurrentUser()
        );
    }

    // ==================== PASSWORD ====================

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody EmailRequest request) {

        authService.forgotPassword(request.getEmail());

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(MessageResponse.of(
                        "If an account exists for that address, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletResponse response) {

        authService.resetPassword(request);

        refreshTokenCookieService.clear(response);

        return ResponseEntity.ok(
                MessageResponse.of("Password reset successfully. Please sign in with your new password.")
        );
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletResponse response) {

        authService.changePassword(request);

        refreshTokenCookieService.clear(response);

        return ResponseEntity.ok(
                MessageResponse.of("Password changed successfully. Please sign in again.")
        );
    }

    // ==================== INTERNAL ====================

    /**
     * Body first, cookie second.
     */
    private Optional<String> resolveRefreshToken(RefreshTokenRequest request,
                                                 HttpServletRequest httpRequest) {

        String bodyToken = request == null ? null : request.getRefreshToken();

        return refreshTokenCookieService.resolve(httpRequest, bodyToken);
    }
}
