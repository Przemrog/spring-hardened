package com.example.notes.config;

import com.example.notes.security.JwtAuthFilter;
import com.example.notes.security.JwtUtil;
import com.example.notes.security.LoginRateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    private final LoginRateLimitFilter rateLimit = new LoginRateLimitFilter();

    // Warstwa /api - bezstanowa, uwierzytelnianie tokenem JWT.
    @Bean
    @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http, JwtUtil jwtUtil) throws Exception {
        JwtAuthFilter jwtFilter = new JwtAuthFilter(jwtUtil);
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/login").permitAll()
                .anyRequest().authenticated())
            // [HARDENING A07/A08]
            // API zwraca 401 zamiast przekierowania do strony HTML.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(
                    (request, response, exception) ->
                        response.sendError(
                            HttpServletResponse.SC_UNAUTHORIZED)))
            .addFilterBefore(
                rateLimit,
                UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // Warstwa webowa - formularz logowania. CSRF pozostaje wlaczony (domyslnie w Spring Security).
    @Bean
    @Order(2)
    public SecurityFilterChain webChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/register", "/login", "/css/**", "/avatars/**").permitAll()
                // [HARDENING A01] eskalacja pionowa zamknieta - /admin wylacznie dla roli ADMIN
                .requestMatchers("/admin").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/notes", true)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login"))
            // [HARDENING A06/A07] limit prob logowania
            .addFilterBefore(rateLimit, UsernamePasswordAuthenticationFilter.class)
            // [HARDENING A02] naglowki bezpieczenstwa (CSP, HSTS, Referrer-Policy);
            // X-Frame-Options i X-Content-Type-Options Spring dodaje domyslnie.
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; frame-ancestors 'none'; object-src 'none'"))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
