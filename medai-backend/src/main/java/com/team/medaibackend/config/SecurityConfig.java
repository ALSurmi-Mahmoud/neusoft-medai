package com.team.medaibackend.config;

import com.team.medaibackend.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/health", "/api/redis/ping").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // Admin only
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/audit/**").hasRole("ADMIN")

                        // Nurse/Technician specific
                        .requestMatchers("/api/nurse/**").hasAnyRole("ADMIN", "NURSE", "TECHNICIAN")

                        // Appointments - accessible by multiple roles
                        .requestMatchers("/api/appointments/**").hasAnyRole("ADMIN", "DOCTOR", "NURSE", "PATIENT")

                        // Studies - accessible by medical staff
                        .requestMatchers("/api/studies/**").hasAnyRole("ADMIN", "DOCTOR", "NURSE", "TECHNICIAN", "RESEARCHER")

                        // Images/Upload - accessible by medical staff
                        .requestMatchers("/api/images/**").hasAnyRole("ADMIN", "DOCTOR", "NURSE", "TECHNICIAN")

                        // Reports
                        .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "DOCTOR", "RESEARCHER")

                        // Worklist
                        .requestMatchers("/api/worklist/**").hasAnyRole("ADMIN", "DOCTOR")

                        // Patients
                        .requestMatchers("/api/patients/**").hasAnyRole("ADMIN", "DOCTOR", "NURSE")

                        // PACS operations
                        .requestMatchers("/api/pacs/**").hasAnyRole("ADMIN", "TECHNICIAN")

                        // AI endpoints
                        .requestMatchers("/api/ai/**").hasAnyRole("ADMIN", "DOCTOR", "RESEARCHER")

                        // Fallback - require authentication for all other requests
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:8081",
                "http://localhost:3000"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}