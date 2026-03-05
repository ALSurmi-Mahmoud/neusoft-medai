package com.team.medaibackend.web;

import com.team.medaibackend.entity.User;
import com.team.medaibackend.service.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/user-management")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<User> users = adminUserService.getAllUsers(PageRequest.of(page, size));

            Map<String, Object> response = new HashMap<>();
            response.put("content", users.getContent().stream()
                    .map(this::buildUserDto)
                    .collect(Collectors.toList()));
            response.put("totalElements", users.getTotalElements());
            response.put("totalPages", users.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> request) {
        try {
            User user = new User();
            user.setUsername((String) request.get("username"));
            user.setPassword((String) request.get("password"));
            user.setFullName((String) request.get("fullName"));
            user.setEmail((String) request.get("email"));
            user.setPhone((String) request.get("phone"));

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) request.get("roles");
            Set<String> roleNames = new HashSet<>(roles);

            User created = adminUserService.createUser(user, roleNames);
            return ResponseEntity.ok(buildUserDto(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            User updates = new User();
            if (request.containsKey("fullName")) {
                updates.setFullName((String) request.get("fullName"));
            }
            if (request.containsKey("email")) {
                updates.setEmail((String) request.get("email"));
            }
            if (request.containsKey("phone")) {
                updates.setPhone((String) request.get("phone"));
            }

            Set<String> roleNames = null;
            if (request.containsKey("roles")) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) request.get("roles");
                roleNames = new HashSet<>(roles);
            }

            User updated = adminUserService.updateUser(id, updates, roleNames);
            return ResponseEntity.ok(buildUserDto(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            adminUserService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            adminUserService.resetPassword(id, request.get("newPassword"));
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    private Map<String, Object> buildUserDto(User user) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", user.getId());
        dto.put("username", user.getUsername());
        dto.put("fullName", user.getFullName());
        dto.put("email", user.getEmail());
        dto.put("phone", user.getPhone());
        dto.put("roles", user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList()));
        dto.put("createdAt", user.getCreatedAt());
        return dto;
    }
}