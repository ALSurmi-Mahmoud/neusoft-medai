package com.team.medaibackend.web;

import com.team.medaibackend.dto.LoginRequest;
import com.team.medaibackend.dto.LoginResponse;
import com.team.medaibackend.dto.RegisterRequest;
import com.team.medaibackend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            LoginResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                throw new RuntimeException("Refresh token is required");
            }
            LoginResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/roles")
    public ResponseEntity<Map<String, Object>> getAvailableRoles() {
        Map<String, Object> response = new HashMap<>();
        response.put("roles", List.of(
                Map.of("value", "ADMIN", "label", "Administrator", "description", "Full system access"),
                Map.of("value", "DOCTOR", "label", "Doctor/Radiologist", "description", "View images, create reports, AI analysis"),
                Map.of("value", "NURSE", "label", "Nurse/Technician", "description", "Upload images, schedule appointments"),
                Map.of("value", "PATIENT", "label", "Patient", "description", "View own reports and appointments"),
                Map.of("value", "RESEARCHER", "label", "Researcher", "description", "Model training, data export")
        ));
        return ResponseEntity.ok(response);
    }
}