// file: src/main/java/com/team/medaibackend/config/SecurityConfig.java
package com.team.medaibackend.config;

import com.team.medaibackend.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

                        // ✅ WebSocket endpoints (authentication handled in WebSocket interceptor)
                        .requestMatchers("/ws/**").permitAll()

                        // ✅ NEW: Users endpoint - allow GET for searching doctors (patients need this!)
                        .requestMatchers(HttpMethod.GET, "/api/users").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/{id}").authenticated()

                        // Admin routes
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/audit/**").hasRole("ADMIN")

                        // Appointments
                        .requestMatchers("/api/appointments", "/api/appointments/**").authenticated()

                        // Patients
                        .requestMatchers("/api/patients", "/api/patients/**").hasAnyRole("ADMIN", "DOCTOR", "NURSE", "TECHNICIAN")

                        // Studies
                        .requestMatchers("/api/studies/**").hasAnyRole("ADMIN", "DOCTOR", "NURSE", "TECHNICIAN", "RESEARCHER")

                        // Images and uploads
                        .requestMatchers("/api/images/**").hasAnyRole("ADMIN", "DOCTOR", "NURSE", "TECHNICIAN")
                        .requestMatchers("/api/uploads/**").hasAnyRole("ADMIN", "DOCTOR", "NURSE", "TECHNICIAN")

                        // Reports - doctors create, patients can view their own
                        .requestMatchers(HttpMethod.POST, "/api/reports/**").hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers(HttpMethod.PUT, "/api/reports/**").hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/reports/**").hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/reports/**").hasAnyRole("ADMIN", "DOCTOR", "RESEARCHER", "PATIENT")

                        // Worklist
                        .requestMatchers("/api/worklist/**").hasAnyRole("ADMIN", "DOCTOR")

                        // Nurse routes
                        .requestMatchers("/api/nurse/**").hasAnyRole("ADMIN", "NURSE", "TECHNICIAN")

                        // PACS
                        .requestMatchers("/api/pacs/**").hasAnyRole("ADMIN", "TECHNICIAN")

                        // AI and DICOM
                        .requestMatchers("/api/ai/**").hasAnyRole("ADMIN", "DOCTOR", "RESEARCHER")
                        .requestMatchers("/api/dicom/**", "/api/storage/**").hasAnyRole("ADMIN", "DOCTOR", "TECHNICIAN")

                        // ✅ Messages and Notifications - All authenticated users can access
                        .requestMatchers("/api/conversations/**").authenticated()
                        .requestMatchers("/api/messages/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()

                        // ✅ Clinical notes and treatment plans
                        .requestMatchers("/api/clinical-notes/**").hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers("/api/treatment-plans/**").hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers("/api/prescriptions/**").hasAnyRole("DOCTOR", "ADMIN", "PATIENT")

                        // ✅ Patient-specific routes
                        .requestMatchers("/api/patient/doctor/**").hasAnyRole("DOCTOR", "ADMIN")

                        // All other requests require authentication
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