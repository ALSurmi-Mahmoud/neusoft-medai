package com.team.medaibackend.web;

import com.team.medaibackend.entity.User;
import com.team.medaibackend.repository.UserRepository;
import com.team.medaibackend.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public UserController(UserRepository userRepository, SecurityUtils securityUtils) {
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Search users by role
     * Used for patient searching for doctors
     * GET /api/users?role=DOCTOR&search=john
     */
    @GetMapping
    public ResponseEntity<?> searchUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search
    ) {
        try {
            // ✅ For patient searching doctors: /api/users?role=DOCTOR&search=...
            if (role != null && role.equalsIgnoreCase("DOCTOR")) {
                List<User> doctors = userRepository.findByRoleNameOrderByFullNameAsc("DOCTOR");

                // Apply search filter if provided
                if (search != null && !search.trim().isEmpty()) {
                    String searchLower = search.trim().toLowerCase();
                    doctors = doctors.stream()
                            .filter(u ->
                                    (u.getFullName() != null && u.getFullName().toLowerCase().contains(searchLower)) ||
                                            (u.getUsername() != null && u.getUsername().toLowerCase().contains(searchLower)) ||
                                            (u.getEmail() != null && u.getEmail().toLowerCase().contains(searchLower))
                            )
                            .collect(Collectors.toList());
                }

                List<Map<String, Object>> result = doctors.stream()
                        .map(this::buildUserDto)
                        .collect(Collectors.toList());

                return ResponseEntity.ok(result);
            }

            // For other cases, require admin
            if (!securityUtils.isAdmin()) {
                return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
            }

            // General search for admins
            List<User> users;
            if (search != null && !search.trim().isEmpty()) {
                users = userRepository.findAll().stream()
                        .filter(u ->
                                u.getFullName().toLowerCase().contains(search.toLowerCase()) ||
                                        u.getUsername().toLowerCase().contains(search.toLowerCase())
                        )
                        .collect(Collectors.toList());
            } else {
                users = userRepository.findAll();
            }

            List<Map<String, Object>> result = users.stream()
                    .map(this::buildUserDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Get current user info
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            User user = securityUtils.getCurrentUserOrThrow();
            return ResponseEntity.ok(buildUserDto(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(buildUserDto(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Helper method to build user DTO (without password)
    private Map<String, Object> buildUserDto(User user) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", user.getId());
        dto.put("username", user.getUsername());
        dto.put("email", user.getEmail());
        dto.put("fullName", user.getFullName());
        dto.put("role", user.getPrimaryRole());
        dto.put("phone", user.getPhone());
        dto.put("department", user.getDepartment());
        dto.put("isActive", user.isEnabled());
        dto.put("createdAt", user.getCreatedAt());
        return dto;
    }
}