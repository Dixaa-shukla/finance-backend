package com.finance_backend.auth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

 /**
 * Enables @PreAuthorize for admin API protection and keeps method security
 * controlled by the same finance.auth.enforce flag, with security enabled by default.
 */

@Configuration
@EnableMethodSecurity
@ConditionalOnProperty(name = "finance.auth.enforce", havingValue = "true", matchIfMissing = true)
public class MethodSecurityConfig {
    // Annotation-only configuration -- @EnableMethodSecurity does the work.
}
