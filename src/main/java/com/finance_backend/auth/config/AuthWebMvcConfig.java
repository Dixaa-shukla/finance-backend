package com.finance_backend.auth.config;

import com.finance_backend.auth.security.UserOwnershipInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuthWebMvcConfig implements WebMvcConfigurer {

    private final UserOwnershipInterceptor userOwnershipInterceptor;

    public AuthWebMvcConfig(UserOwnershipInterceptor userOwnershipInterceptor) {
        this.userOwnershipInterceptor = userOwnershipInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userOwnershipInterceptor)
                .addPathPatterns("/api/**")
                // /api/v1/auth/** is public and has no {userId} anywhere, but
                // excluding it makes the intent explicit and saves the check.
                .excludePathPatterns("/api/v1/auth/**");
    }
}
