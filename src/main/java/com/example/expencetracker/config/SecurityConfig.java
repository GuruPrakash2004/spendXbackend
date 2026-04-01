package com.example.expencetracker.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
 
/**
 * Spring Security configuration.
 *
 * Phase 1 (current): All API endpoints are open — no authentication required.
 * This lets us focus on building and testing CRUD without JWT getting in the way.
 *
 * Phase 2 (future): Replace permitAll() with JWT filter chain:
 *   - Add a JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter.
 *   - Restrict all /api/** routes to authenticated users only.
 *   - Expose /api/auth/login as a public endpoint.
 *
 * CSRF is disabled because this is a stateless REST API:
 *   - No sessions, no cookies → CSRF attacks are not applicable.
 *   - Leaving CSRF enabled would require a token header on every POST/PUT/DELETE.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
 
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — not needed for stateless REST APIs
            .csrf(AbstractHttpConfigurer::disable)
 
            // Phase 1: open all endpoints so Postman testing is friction-free
            .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll()
            );
 
        return http.build();
    }
}