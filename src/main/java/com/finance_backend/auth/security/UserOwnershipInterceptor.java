package com.finance_backend.auth.security;

import com.finance_backend.common.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class UserOwnershipInterceptor implements HandlerInterceptor {

    private static final String USER_ID_VARIABLE = "userId";

    private final boolean enforce;

    public UserOwnershipInterceptor(@Value("${finance.auth.enforce:true}") boolean enforce) {
        this.enforce = enforce;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        if (!enforce) {
            return true;
        }

        Long requestedUserId = extractUserIdPathVariable(request);
        if (requestedUserId == null) {
            return true;
        }

        Optional<AuthPrincipal> caller = SecurityUtils.currentPrincipal();

        if (caller.isEmpty()) {
            return true;
        }

        AuthPrincipal principal = caller.get();

        // Admins legitimately read other users -- e.g. GET /api/v1/admin/users/{userId}.
        if (principal.hasAdminRole()) {
            return true;
        }

        if (!requestedUserId.equals(principal.userId())) {
            log.warn("Ownership violation: userId={} tried to access userId={} via {} {}",
                    principal.userId(), requestedUserId, request.getMethod(), request.getRequestURI());

            throw new ForbiddenException("You may only access your own data");
        }

        return true;
    }

    private Long extractUserIdPathVariable(HttpServletRequest request) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        if (!(attribute instanceof Map<?, ?> variables)) {
            return null;
        }

        Object raw = variables.get(USER_ID_VARIABLE);
        if (raw == null) {
            return null;
        }

        try {
            return Long.valueOf(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
