package com.example.notes.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// [HARDENING A09]
@Component
public class AuthEventLogger {

    private static final Logger log =
            LoggerFactory.getLogger(AuthEventLogger.class);

    @EventListener
    public void onFailure(
            AbstractAuthenticationFailureEvent event) {

        Authentication authentication =
                event.getAuthentication();

        String user = authentication != null
                ? authentication.getName()
                : "unknown";

        HttpServletRequest request = currentRequest();

        log.warn(
                "SECURITY event=authentication_failure "
                        + "user={} ip={} method={} path={} reason={}",
                user,
                remoteAddress(request),
                method(request),
                path(request),
                event.getException()
                        .getClass()
                        .getSimpleName()
        );
    }

    @EventListener
    public void onDenied(
            AuthorizationDeniedEvent<?> event) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String user = authentication != null
                ? authentication.getName()
                : "anonymous";

        HttpServletRequest request = currentRequest();

        log.warn(
                "SECURITY event=authorization_denied "
                        + "user={} ip={} method={} path={}",
                user,
                remoteAddress(request),
                method(request),
                path(request)
        );
    }

    private HttpServletRequest currentRequest() {
        var attributes =
                RequestContextHolder
                        .getRequestAttributes();

        if (attributes
                instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }

        return null;
    }

    private String remoteAddress(
            HttpServletRequest request) {

        return request != null
                ? request.getRemoteAddr()
                : "unknown";
    }

    private String method(
            HttpServletRequest request) {

        return request != null
                ? request.getMethod()
                : "unknown";
    }

    private String path(
            HttpServletRequest request) {

        return request != null
                ? request.getRequestURI()
                : "unknown";
    }
}