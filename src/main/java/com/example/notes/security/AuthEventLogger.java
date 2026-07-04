package com.example.notes.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

// [HARDENING A09] audyt zdarzen bezpieczenstwa przez zdarzenia Spring Security.
@Component
public class AuthEventLogger {

    private static final Logger log = LoggerFactory.getLogger(AuthEventLogger.class);

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        Object principal = event.getAuthentication() != null ? event.getAuthentication().getName() : "?";
        log.warn("Nieudane uwierzytelnienie dla '{}': {}", principal,
                event.getException().getMessage());
    }

    @EventListener
    public void onDenied(AuthorizationDeniedEvent<?> event) {
        log.warn("Odmowa dostepu do zasobu chronionego.");
    }
}
