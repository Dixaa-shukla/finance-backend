package com.finance_backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, fieldErrors);
    }

    /**
     * Added while building Module 8 (Transaction Management), which is the
     * first place an invalid enum query param (?type=FOO) was actually
     * exercised in testing -- but this applies retroactively to every
     * existing enum query param across the app (PaymentMethod, IncomeSource,
     * CategoryType, BudgetPeriod, etc.), all of which previously fell
     * through to the generic 500 handler below instead of returning 400.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return build(HttpStatus.BAD_REQUEST, message, req, null);
    }

    /**
     * Malformed or missing request body -- 400, not 500.
     *
     * ⚠️ WITHOUT THIS, A TYPO IN JSON LOOKED LIKE A SERVER CRASH. Jackson throws
     * HttpMessageNotReadableException before @Valid ever runs, so a trailing comma,
     * an unclosed brace, "amount": "abc" where a BigDecimal is expected, or a POST
     * sent with no body at all fell through to handleGeneric() below and returned
     * 500 "An unexpected error occurred" -- telling the caller the server is broken
     * when the request is. It applies to every @RequestBody route in the app.
     *
     * The cause is deliberately NOT included in the message: Jackson's text carries
     * internal class and package names, which is both unhelpful to a frontend and
     * more than an unauthenticated caller should learn about the server.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "Request body is missing or not valid JSON", req, null);
    }

    /**
     * Added while building Module 11 (AI Financial Assistant), which calls
     * an external AI provider (OpenAI). A provider outage or malformed
     * response is a different failure class than bad user input -- 502
     * tells the frontend "retry later" rather than "fix your request."
     * Reusable by any future module that calls an external API.
     */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceError(ExternalServiceException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), req, null);
    }

    /**
     * Added while building Module 1 (Authentication & Security).
     *
     * 401 -- the caller is unknown: bad password, expired/forged JWT, revoked
     * or unknown refresh token. Raised by the auth module's
     * InvalidCredentialsException / InvalidRefreshTokenException.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, null);
    }

    /**
     * Added while building Module 1 (Authentication & Security).
     *
     * 403 -- the caller is known but still not allowed: asking for another
     * user's userId (UserOwnershipInterceptor / UserIdBodyOwnershipAdvice), or
     * asking for a row owned by someone else (ResourceOwnershipGuard).
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req, null);
    }

    /**
     * Added while building Module 1 (Authentication & Security).
     *
     * ⚠️ THIS HANDLER IS NOT OPTIONAL. Spring Security normally converts an
     * AccessDeniedException into 403 inside its ExceptionTranslationFilter --
     * but @PreAuthorize denials on the admin controllers are thrown by an AOP
     * interceptor sitting INSIDE DispatcherServlet, which is *deeper* than
     * that filter. DispatcherServlet resolves the exception through this class
     * first, so without this method every "hasRole('ADMIN')" rejection would
     * be caught by handleGeneric() below and returned as
     * 500 "An unexpected error occurred" instead of 403.
     *
     * Covers AuthorizationDeniedException too -- it extends AccessDeniedException.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to access this resource", req, null);
    }

    /**
     * Added while building Module 1 (Authentication & Security).
     *
     * Spring Security's own AuthenticationException, for the rare case it
     * surfaces inside the dispatcher rather than in the filter chain (where
     * JwtAuthEntryPoint answers instead). The message is deliberately generic
     * so a failed login cannot be used to probe which emails exist.
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            org.springframework.security.core.AuthenticationException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication required", req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", req, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                HttpServletRequest req, Map<String, String> fieldErrors) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(req.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}