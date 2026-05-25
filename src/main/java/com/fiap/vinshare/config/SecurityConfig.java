package com.fiap.vinshare.config;

import com.fiap.vinshare.infra.security.JwtAuthenticationFilter;
import com.fiap.vinshare.infra.security.PayloadIntegrityFilter;
import com.fiap.vinshare.infra.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração central de segurança da API.
 *
 * Cobre os critérios da disciplina de Cybersecurity:
 *  - Autenticação stateless via JWT (filter customizado).
 *  - Autorização baseada em papéis (RBAC) via @PreAuthorize.
 *  - BCrypt para senhas.
 *  - Endpoints públicos somente para auth, docs e health.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/v3/api-docs/**",
            "/docs/**",
            "/swagger-ui/**",
            "/actuator/health",
            "/actuator/info"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final PayloadIntegrityFilter payloadIntegrityFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(
                                org.springframework.http.HttpStatus.UNAUTHORIZED))
                )
                .headers(headers -> headers
                        .frameOptions(f -> f.deny())
                        .contentTypeOptions(c -> {})
                        .referrerPolicy(r -> r.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER
                        ))
                );

        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(payloadIntegrityFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
