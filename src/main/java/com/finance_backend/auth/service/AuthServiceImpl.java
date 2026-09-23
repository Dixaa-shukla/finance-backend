package com.finance_backend.auth.service;

import com.finance_backend.auth.dto.AuthResponse;
import com.finance_backend.auth.dto.ChangePasswordRequest;
import com.finance_backend.auth.dto.LoginRequest;
import com.finance_backend.auth.dto.RegisterRequest;
import com.finance_backend.auth.dto.ResetPasswordRequest;
import com.finance_backend.auth.dto.UserResponse;
import com.finance_backend.auth.entity.AuthProvider;
import com.finance_backend.auth.entity.AuthToken;
import com.finance_backend.auth.entity.AuthTokenType;
import com.finance_backend.auth.entity.RefreshToken;
import com.finance_backend.auth.entity.Role;
import com.finance_backend.auth.entity.User;
import com.finance_backend.auth.exception.EmailAlreadyExistsException;
import com.finance_backend.auth.exception.InvalidCredentialsException;
import com.finance_backend.auth.exception.InvalidRefreshTokenException;
import com.finance_backend.auth.exception.InvalidTokenException;
import com.finance_backend.auth.exception.UserNotFoundException;
import com.finance_backend.auth.repository.AuthTokenRepository;
import com.finance_backend.auth.repository.RefreshTokenRepository;
import com.finance_backend.auth.repository.UserRepository;
import com.finance_backend.auth.security.JwtService;
import com.finance_backend.auth.security.SecurityUtils;
import com.finance_backend.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthEmailService authEmailService;

    private final long refreshTtlSeconds;
    private final long passwordResetMinutes;

    private final Set<String> adminEmails;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            AuthTokenRepository authTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthEmailService authEmailService,
            @Value("${spring.security.jwt.refresh-ttl-seconds:1209600}") long refreshTtlSeconds,
            @Value("${finance.auth.password-reset-minutes:60}") long passwordResetMinutes,
            @Value("${finance.auth.admin-emails:}") String adminEmailsCsv) {

        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authTokenRepository = authTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authEmailService = authEmailService;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.passwordResetMinutes = passwordResetMinutes;
        this.adminEmails = parseAdminEmails(adminEmailsCsv);

        if (adminEmails.isEmpty()) {
            log.warn("finance.auth.admin-emails is empty -- no account will receive ROLE_ADMIN, "
                    + "so /api/v1/admin/** will be unreachable. Set the ADMIN_EMAILS environment variable.");
        } else {
            log.info("Admin bootstrap configured for {} address(es)", adminEmails.size());
        }
    }

    // ==================== REGISTRATION & LOGIN ====================

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw EmailAlreadyExistsException.forEmail(email);
        }

        Set<Role> roles = rolesFor(email);

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .enabled(true)
                .provider(AuthProvider.LOCAL)
                .build();

        User saved = userRepository.save(user);
        log.info("Registered userId={} email={} roles={}", saved.getId(), saved.getEmail(), roles);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(InvalidCredentialsException::forLogin);


        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw InvalidCredentialsException.forLogin();
        }

        // Checked only AFTER the password, so the state of an account cannot be
        // probed without knowing its password.
        if (!user.isEnabled()) {
            throw InvalidCredentialsException.forDisabledAccount();
        }

        log.info("Login succeeded for userId={}", user.getId());
        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(String email, String providerId, boolean emailVerified) {
        String normalized = normalizeEmail(email);

        Optional<User> byProvider = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, providerId);

        User user = byProvider.orElseGet(() -> linkOrCreateGoogleAccount(normalized, providerId, emailVerified));

        if (!user.isEnabled()) {
            throw InvalidCredentialsException.forDisabledAccount();
        }

        log.info("Google sign-in succeeded for userId={}", user.getId());
        return issueTokens(user);
    }

    // ==================== SESSION ====================

    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken existing = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(InvalidRefreshTokenException::create);

        if (!existing.isUsable()) {
            throw InvalidRefreshTokenException.create();
        }

        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(InvalidRefreshTokenException::create);

        if (!user.isEnabled()) {
            throw InvalidRefreshTokenException.create();
        }

        /*
         * If a token is ever stolen, whichever party uses it second gets a 401
         * instead of both keeping a working session indefinitely.
         */
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        log.debug("Rotated refresh token for userId={}", user.getId());
        return issueTokens(user);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Logged out userId={}", token.getUserId());
        });
        // Unknown token -> no exception. Logout is idempotent, and a 404 here
        // would tell an attacker which tokens are real.
    }

    @Override
    @Transactional
    public void logoutAll() {
        Long userId = SecurityUtils.currentUserIdOrThrow();
        int revoked = refreshTokenRepository.revokeAllForUser(userId);
        log.info("Logged out all sessions for userId={} ({} token(s) revoked)", userId, revoked);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Long userId = SecurityUtils.currentUserIdOrThrow();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.forId(userId));

        return toResponse(user);
    }

    // ==================== PASSWORD ====================

    @Override
    @Transactional
    public void forgotPassword(String email) {
        Optional<User> found = userRepository.findByEmailIgnoreCase(normalizeEmail(email));

        // Silent for unknown addresses, and for Google-only accounts -- there is
        // no password to reset, and saying so would confirm the account exists.
        if (found.isEmpty() || found.get().getPassword() == null) {
            log.debug("Password reset ignored for {} (unknown or passwordless account)", email);
            return;
        }

        User user = found.get();

        authTokenRepository.invalidateOutstanding(
                user.getId(), AuthTokenType.PASSWORD_RESET, LocalDateTime.now());

        AuthToken token = saveAuthToken(
                user.getId(),
                AuthTokenType.PASSWORD_RESET,
                LocalDateTime.now().plusMinutes(passwordResetMinutes));

        authEmailService.sendPasswordResetEmail(user.getEmail(), token.getToken());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        AuthToken authToken = authTokenRepository
                .findByTokenAndTokenType(request.getToken(), AuthTokenType.PASSWORD_RESET)
                .orElseThrow(InvalidTokenException::forPasswordReset);

        if (!authToken.isUsable()) {
            throw InvalidTokenException.forPasswordReset();
        }

        User user = userRepository.findById(authToken.getUserId())
                .orElseThrow(InvalidTokenException::forPasswordReset);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        authToken.setUsedAt(LocalDateTime.now());
        authTokenRepository.save(authToken);

        // Whoever prompted the reset may have had the old password. Kill every
        // session opened with it.
        int revoked = refreshTokenRepository.revokeAllForUser(user.getId());

        log.info("Password reset for userId={} ({} session(s) revoked)", user.getId(), revoked);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId = SecurityUtils.currentUserIdOrThrow();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.forId(userId));

        if (user.getPassword() == null) {
            // Safe to be specific: the caller is provably the account owner.
            throw new BadRequestException(
                    "This account signs in with Google and has no password to change");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw InvalidCredentialsException.forCurrentPassword();
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("newPassword must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        int revoked = refreshTokenRepository.revokeAllForUser(userId);

        log.info("Password changed for userId={} ({} session(s) revoked)", userId, revoked);
    }

    // ==================== INTERNAL: TOKEN ISSUING ====================

    /**
     * Build the access + refresh pair for an authenticated account.
     */
    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);

        RefreshToken refreshToken = refreshTokenRepository.save(RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(user.getId())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTtlSeconds))
                .revoked(false)
                .build());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .roles(roleNames(user))
                .build();
    }

    private AuthToken saveAuthToken(Long userId, AuthTokenType type, LocalDateTime expiresAt) {
        return authTokenRepository.save(AuthToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .tokenType(type)
                .expiresAt(expiresAt)
                .build());
    }

    // ==================== INTERNAL: GOOGLE ACCOUNT LINKING ====================

    private User linkOrCreateGoogleAccount(String email, String providerId, boolean emailVerified) {
        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(email);

        if (byEmail.isPresent()) {
            User existing = byEmail.get();

            if (!emailVerified) {
                log.warn("Refusing to link Google identity to existing userId={} -- "
                        + "Google reports the address as unverified", existing.getId());
                throw InvalidCredentialsException.forLogin();
            }

            existing.setProvider(AuthProvider.GOOGLE);
            existing.setProviderId(providerId);

            log.info("Linked Google identity to existing userId={}", existing.getId());
            return userRepository.save(existing);
        }

        User created = userRepository.save(User.builder()
                .email(email)
                .password(null)                 // no local password -- Google only
                .roles(rolesFor(email))
                .enabled(true)
                .provider(AuthProvider.GOOGLE)
                .providerId(providerId)
                .build());

        log.info("Created account from Google sign-in: userId={} email={}", created.getId(), created.getEmail());
        return created;
    }

    // ==================== INTERNAL: HELPERS & MAPPERS ====================

    /**
     * Trim and lower-case. Storing a canonical form means the users_email unique
     * constraint and findByEmailIgnoreCase agree with each other.
     */
    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private Set<String> parseAdminEmails(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<Role> rolesFor(String normalizedEmail) {
        Set<Role> roles = new LinkedHashSet<>();
        roles.add(Role.USER);

        if (adminEmails.contains(normalizedEmail)) {
            roles.add(Role.ADMIN);
            log.info("Granting ROLE_ADMIN to {} (listed in finance.auth.admin-emails)", normalizedEmail);
        }
        return roles;
    }

    private Set<String> roleNames(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return Set.of(Role.USER.name());
        }
        return user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .roles(roleNames(user))
                .enabled(user.isEnabled())
                .provider(user.getProvider() == null ? null : user.getProvider().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
