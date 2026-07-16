package com.example.notes.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Nie jest @Component - tworzony jawnie w SecurityConfig, aby nie rejestrowal sie
// automatycznie w glownym lancuchu filtrow (dotyczy tylko warstwy /api).
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwt;

    private static final Logger log = LoggerFactory.getLogger(
            JwtAuthFilter.class);

    public JwtAuthFilter(JwtUtil jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                var claims = jwt.parse(header.substring(7));
                String userId = claims.getSubject();
                var auth = new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ex) {
                /*
                 * [HARDENING A09]
                 * Nie zapisujemy tokenu ani nagłówka Authorization.
                 */
                log.warn(
                        "SECURITY event=jwt_validation_failure "
                                + "reason={} ip={} method={} path={}",
                        ex.getClass().getSimpleName(),
                        req.getRemoteAddr(),
                        req.getMethod(),
                        req.getRequestURI());

                // Token nieprawidłowy – kontekst pozostaje
                // nieuwierzytelniony.
            }
        }
        chain.doFilter(req, res);
    }
}
