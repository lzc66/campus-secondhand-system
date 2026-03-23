package com.campus.secondhand.config;

import com.campus.secondhand.common.api.ApiResponse;
import com.campus.secondhand.security.JwtAuthenticationFilter;
import com.campus.secondhand.security.JwtProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final ObjectMapper SECURITY_OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/v1/admin/auth/login",
                                "/api/v1/user/auth/login",
                                "/api/v1/public/**",
                                "/uploads/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/admin/init/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/v1/admin/registration-applications/**").hasAnyRole("SUPER_ADMIN", "AUDITOR")
                        .requestMatchers("/api/v1/admin/announcements/**").hasAnyRole("SUPER_ADMIN", "AUDITOR", "OPERATOR")
                        .requestMatchers("/api/v1/admin/auth/me").hasAnyRole("SUPER_ADMIN", "AUDITOR", "OPERATOR")
                        .requestMatchers("/api/v1/user/**").hasRole("USER")
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> writeError(response, HttpStatus.UNAUTHORIZED, 40100, "Unauthorized"))
                        .accessDeniedHandler((request, response, accessDeniedException) -> writeError(response, HttpStatus.FORBIDDEN, 40300, "Forbidden")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeError(jakarta.servlet.http.HttpServletResponse response, HttpStatus status, int code, String message)
            throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        SECURITY_OBJECT_MAPPER.writeValue(response.getWriter(), ApiResponse.error(code, message));
    }
}