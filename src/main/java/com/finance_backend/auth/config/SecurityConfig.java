package com.finance_backend.auth.config;

import com.finance_backend.auth.security.JwtAccessDeniedHandler;
import com.finance_backend.auth.security.JwtAuthEntryPoint;
import com.finance_backend.auth.security.JwtAuthenticationFilter;
import com.finance_backend.auth.security.JwtService;
import com.finance_backend.auth.security.OAuth2LoginSuccessHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
/**
 * Defines the application's main SecurityFilterChain, replacing Spring Security's
 * default HTTP Basic security and safely configuring authentication, authorization,
 * and required beans without creating circular dependencies.
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Endpoints reachable with no token at all. */
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/**",          // register, login, refresh, logout, forgot/reset password
            "/oauth2/**",               // Spring's Google sign-in entry point
            "/login/oauth2/**",         // Google's callback
            "/v3/api-docs/**",          // SpringDoc
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error"                    // Boot's error forward -- never gate it
    };


    private static final String[] AUTHENTICATED_AUTH_PATHS = {
            "/api/v1/auth/me",
            "/api/v1/auth/logout-all",
            "/api/v1/auth/change-password"
    };

    private final boolean enforce;
    private final String frontendRedirectUri;

    public SecurityConfig(
            @Value("${finance.auth.enforce:true}") boolean enforce,
            @Value("${finance.auth.frontend-redirect-uri:http://localhost:5173/oauth2/callback}")
            String frontendRedirectUri) {

        this.enforce = enforce;
        this.frontendRedirectUri = frontendRedirectUri;
    }

    // ==================== PASSWORD HASHING ====================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ==================== FILTER CHAIN ====================

    /**
     * @param http                the builder
     * @param jwtService          used to construct the JWT filter (see below)
     * @param entryPoint          writes 401 JSON
     * @param accessDeniedHandler writes 403 JSON
     * @param clientRegistrations present ONLY when Google credentials are configured
     * @param oAuth2SuccessHandler mints our tokens after a Google login
     * @return the one and only filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            JwtAuthEntryPoint entryPoint,
            JwtAccessDeniedHandler accessDeniedHandler,
            ObjectProvider<ClientRegistrationRepository> clientRegistrations,
            ObjectProvider<OAuth2LoginSuccessHandler> oAuth2SuccessHandler) throws Exception {

        /*
         * ⚠️ The JWT filter is created here instead of being injected or marked as
         * @Component. This prevents it from running twice for the same request.
         */
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService);

        http
                /* CSRF is disabled because the API uses JWT tokens in the Authorization header instead of cookies. */

                .csrf(csrf -> csrf.disable())

                /* CORS uses the existing policy from CorsConfig, so there is only one CORS configuration. */

                .cors(Customizer.withDefaults())

                // No JSESSIONID for API calls; every request re-authenticates
                // from its Bearer token.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // The API answers with JSON, never a login form or a browser
                // Basic-auth popup.
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())   // logout is POST /api/v1/auth/logout

                /* Unauthenticated API requests return 401 JSON instead of redirecting to Google;
                 Google login starts only through the OAuth2 login URL. */

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        if (enforce) {
            http.authorizeHttpRequests(auth -> auth
                    // Browser preflight carries no Authorization header by
                    // definition, so it must never require one.
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // Before PUBLIC_PATHS -- see the note on the constant.
                    .requestMatchers(AUTHENTICATED_AUTH_PATHS).authenticated()
                    .requestMatchers(PUBLIC_PATHS).permitAll()
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated());
        } else {
            /* In development, setting finance.auth.enforce=false makes all endpoints public,
             * while JWT tokens still work; never disable this security setting in production.
             */

            log.warn("⚠️  finance.auth.enforce=false -- ALL endpoints are PUBLIC and ownership "
                    + "checks are OFF. Development only.");

            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }

        configureGoogleLogin(http, clientRegistrations, oAuth2SuccessHandler);

        return http.build();
    }

    // ==================== GOOGLE SIGN-IN (CONDITIONAL) ====================


    private void configureGoogleLogin(HttpSecurity http,
                                      ObjectProvider<ClientRegistrationRepository> clientRegistrations,
                                      ObjectProvider<OAuth2LoginSuccessHandler> oAuth2SuccessHandler) throws Exception {

        if (clientRegistrations.getIfAvailable() == null) {
            log.info("Google sign-in is DISABLED (no spring.security.oauth2.client.registration.google.* "
                    + "properties). Email/password authentication is unaffected.");
            return;
        }

        OAuth2LoginSuccessHandler successHandler = oAuth2SuccessHandler.getObject();

        http.oauth2Login(oauth -> oauth
                .successHandler(successHandler)
                // A failure must not render Spring's default HTML error page --

                .failureUrl(frontendRedirectUri + "?error=oauth2_failed"));

        log.info("Google sign-in is ENABLED -- start it at GET /oauth2/authorization/google");
    }
}
