package com.example.notes.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleStatus(
            ResponseStatusException ex) {

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ex.getReason() == null
                        ? "Błąd"
                        : ex.getReason());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAny(
            Exception ex) {

        /*
         * [HARDENING A09/A10]
         * Szczegóły są zapisywane po stronie serwera,
         * lecz nie są ujawniane klientowi.
         */
        log.error(
                "Nieobsłużony wyjątek aplikacji",
                ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        "Wystąpił nieoczekiwany błąd. "
                        + "Zdarzenie zostało zarejestrowane.");
    }
}