package com.example.notes.security;

import com.example.notes.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final String issuer;
    private final String audience;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.issuer}") String issuer,
                   @Value("${app.jwt.audience}") String audience) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.audience = audience;
    }

    public String generate(User user) {
        // [HARDENING A08] token krotkozyjacy (1 h) z jawnym wydawca i odbiorca
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(new Date(now))
                .expiration(new Date(now + 60 * 60 * 1000L))
                .signWith(key)
                .compact();
    }

    // [HARDENING A08] pelna walidacja: podpis, czas zycia, wydawca oraz odbiorca
    public Claims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (claims.getAudience() == null || !claims.getAudience().contains(audience)) {
            throw new IllegalArgumentException("Nieprawidlowy odbiorca tokenu (aud).");
        }
        return claims;
    }
}
