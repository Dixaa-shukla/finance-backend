package com.finance_backend.auth.security;

import com.finance_backend.common.exception.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class ResourceOwnershipGuard {

    private final boolean enforce;

    public ResourceOwnershipGuard(@Value("${finance.auth.enforce:true}") boolean enforce) {
        this.enforce = enforce;
    }

    public void check(Long ownerUserId, String resource, Long resourceId) {
        if (ownerUserId == null) {
            return;
        }
        AuthPrincipal caller = callerToCheck();
        if (caller == null) {
            return;
        }
        if (!ownerUserId.equals(caller.userId())) {
            log.warn("Ownership violation: userId={} tried to access {} id={} owned by userId={}",
                    caller.userId(), resource, resourceId, ownerUserId);
            throw new ForbiddenException("You may only access your own data");
        }
    }

    public void requireAdmin(String resource, Long resourceId) {
        AuthPrincipal caller = callerToCheck();
        if (caller == null) {
            return;
        }
        log.warn("Ownership violation: userId={} tried to modify shared {} id={} without the ADMIN role",
                caller.userId(), resource, resourceId);
        throw new ForbiddenException("Only an administrator may change this");
    }

    private AuthPrincipal callerToCheck() {
        if (!enforce) {
            return null;
        }
        Optional<AuthPrincipal> caller = SecurityUtils.currentPrincipal();
        if (caller.isEmpty()) {
            return null;
        }
        AuthPrincipal principal = caller.get();
        if (principal.hasAdminRole()) {
            return null;
        }
        return principal;
    }
}
