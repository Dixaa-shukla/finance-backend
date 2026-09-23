package com.finance_backend.auth.security;

import com.finance_backend.common.exception.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ControllerAdvice
public class UserIdBodyOwnershipAdvice implements RequestBodyAdvice {

    private static final String GETTER_NAME = "getUserId";

    private static final Map<Class<?>, Optional<Method>> GETTER_CACHE = new ConcurrentHashMap<>();

    private final boolean enforce;

    public UserIdBodyOwnershipAdvice(@Value("${finance.auth.enforce:true}") boolean enforce) {
        this.enforce = enforce;
    }

    @Override
    public boolean supports(MethodParameter methodParameter,
                            Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return enforce;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage,
                                          MethodParameter parameter,
                                          Type targetType,
                                          Class<? extends HttpMessageConverter<?>> converterType) {
        return inputMessage;
    }

    @Override
    public Object afterBodyRead(Object body,
                               HttpInputMessage inputMessage,
                               MethodParameter parameter,
                               Type targetType,
                               Class<? extends HttpMessageConverter<?>> converterType) {

        if (body != null) {
            assertOwnership(body);
        }
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body,
                                  HttpInputMessage inputMessage,
                                  MethodParameter parameter,
                                  Type targetType,
                                  Class<? extends HttpMessageConverter<?>> converterType) {
        // No body, nothing to own. @NotNull validation reports it as a 400.
        return body;
    }

    private void assertOwnership(Object body) {

        Optional<Method> getter = GETTER_CACHE.computeIfAbsent(
                body.getClass(),
                type -> Optional.ofNullable(ReflectionUtils.findMethod(type, GETTER_NAME)));

        if (getter.isEmpty()) {
            return;
        }

        Optional<AuthPrincipal> caller = SecurityUtils.currentPrincipal();
        if (caller.isEmpty()) {
            return;
        }

        AuthPrincipal principal = caller.get();
        if (principal.hasAdminRole()) {
            return;
        }

        Method method = getter.get();
        ReflectionUtils.makeAccessible(method);
        Object value = ReflectionUtils.invokeMethod(method, body);

        if (!(value instanceof Number number)) {
            return;
        }

        long bodyUserId = number.longValue();
        if (bodyUserId != principal.userId()) {
            log.warn("Ownership violation: userId={} sent a {} body carrying userId={}",
                    principal.userId(), body.getClass().getSimpleName(), bodyUserId);

            throw new ForbiddenException("The userId in the request body must be your own");
        }
    }
}
