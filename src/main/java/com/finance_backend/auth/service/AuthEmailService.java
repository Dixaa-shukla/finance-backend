package com.finance_backend.auth.service;

public interface AuthEmailService {

    void sendPasswordResetEmail(String email, String token);
}
