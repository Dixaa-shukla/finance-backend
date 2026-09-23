package com.finance_backend.auth.service;

import com.finance_backend.auth.dto.AuthResponse;
import com.finance_backend.auth.dto.ChangePasswordRequest;
import com.finance_backend.auth.dto.LoginRequest;
import com.finance_backend.auth.dto.RegisterRequest;
import com.finance_backend.auth.dto.ResetPasswordRequest;
import com.finance_backend.auth.dto.UserResponse;

public interface AuthService {

    // ==================== REGISTRATION & LOGIN ====================

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse loginWithGoogle(String email, String providerId, boolean emailVerified);

    // ==================== SESSION ====================

    AuthResponse refresh(String refreshToken);

    void logout(String refreshToken);

    /** Revoke every refresh token for the currently authenticated user. */
    void logoutAll();

    UserResponse getCurrentUser();

    // ==================== PASSWORD ====================

    void forgotPassword(String email);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(ChangePasswordRequest request);
}
