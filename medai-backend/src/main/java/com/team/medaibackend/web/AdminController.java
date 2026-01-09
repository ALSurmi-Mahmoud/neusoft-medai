package com.team.medaibackend.web;

import com.team.medaibackend.entity.User;
import com.team.medaibackend.repository.AuditLogRepository;
import com.team.medaibackend.repository.RoleRepository;
import com.team.medaibackend.repository.UserRepository;
import com.team.medaibackend.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository,
                           RoleRepository roleRepository,
                           AuditLogRepository auditLogRepository,
                           AuditService auditService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> userPage;

        if (search != null && !search.isEmpty()) {
            userPage = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    search, search, pageable);
        } else if (role != null && !role.isEmpty()) {
            userPage = userRepository.findByRoleName(role, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        List<Map<String, Object>> userList = new ArrayList<>();
        for (User user : userPage.getContent()) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("email", user.getEmail());
            userMap.put("fullName", user.getFullName());
            userMap.put("role", user.getPrimaryRole());
            userMap.put("phone", user.getPhone());
            userMap.put("department", user.getDepartment());
            userMap.put("title", user.getTitle());
            userMap.put("enabled", user.isEnabled());
            userMap.put("accountLocked", user.isAccountLocked());
            userMap.put("lastLogin", user.getLastLogin());
            userMap.put("createdAt", user.getCreatedAt());
            userList.add(userMap);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", userList);
        response.put("totalElements", userPage.getTotalElements());
        response.put("totalPages", userPage.getTotalPages());
        response.put("number", userPage.getNumber());
        response.put("size", userPage.getSize());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");
            String email = (String) request.get("email");
            String password = (String) request.get("password");
            String fullName = (String) request.get("fullName");
            String roleName = (String) request.get("role");

            if (userRepository.existsByUsername(username)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Username already exists"));
            }
            if (userRepository.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email already exists"));
            }

            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setFullName(fullName);
            user.setPhone((String) request.get("phone"));
            user.setDepartment((String) request.get("department"));
            user.setTitle((String) request.get("title"));
            user.setEnabled(true);

            // Set role properly
            String roleToAssign = roleName != null ? roleName : "USER";
            roleRepository.findByName(roleToAssign).ifPresent(user::addRole);

            User saved = userRepository.save(user);
            auditService.logAnonymous("CREATE", "USER", saved.getId().toString());

            return ResponseEntity.ok(Map.of("message", "User created", "id", saved.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return userRepository.findById(id).map(user -> {
            if (request.containsKey("fullName")) user.setFullName((String) request.get("fullName"));
            if (request.containsKey("email")) user.setEmail((String) request.get("email"));
            if (request.containsKey("phone")) user.setPhone((String) request.get("phone"));
            if (request.containsKey("department")) user.setDepartment((String) request.get("department"));
            if (request.containsKey("title")) user.setTitle((String) request.get("title"));

            // Handle role change properly
            if (request.containsKey("role")) {
                String newRoleName = (String) request.get("role");
                user.getRoles().clear();
                roleRepository.findByName(newRoleName).ifPresent(user::addRole);
            }

            userRepository.save(user);
            auditService.logAnonymous("UPDATE", "USER", id.toString());

            return ResponseEntity.ok(Map.of("message", "User updated"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Boolean enabled = (Boolean) request.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "enabled field is required"));
        }

        return userRepository.findById(id).map(user -> {
            user.setEnabled(enabled);
            userRepository.save(user);
            auditService.logAnonymous(enabled ? "ENABLE_USER" : "DISABLE_USER", "USER", id.toString());
            return ResponseEntity.ok(Map.of("message", "User status updated"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}/password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String newPassword = request.get("password");
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 6 characters"));
        }

        return userRepository.findById(id).map(user -> {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            auditService.logAnonymous("RESET_PASSWORD", "USER", id.toString());
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);

        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();

        // Prevent deleting the last admin
        if ("ADMIN".equals(user.getPrimaryRole())) {
            long adminCount = userRepository.countByRoleName("ADMIN");
            if (adminCount <= 1) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Cannot delete the last admin user"));
            }
        }

        // Store username before deletion for the response
        String deletedUsername = user.getUsername();

        // Nullify user references in audit logs (preserves audit history)
        auditLogRepository.nullifyUserReferences(id);

        // Log the deletion
        auditService.logAnonymous("DELETE", "USER", id.toString());

        // Actually delete the user
        userRepository.deleteById(id);

        return ResponseEntity.ok(Map.of(
                "message", "User '" + deletedUsername + "' deleted successfully",
                "deletedId", id
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalUsers", userRepository.count());
        stats.put("adminCount", userRepository.countByRoleName("ADMIN"));
        stats.put("doctorCount", userRepository.countByRoleName("DOCTOR"));
        stats.put("nurseCount", userRepository.countByRoleName("NURSE"));
        stats.put("patientCount", userRepository.countByRoleName("PATIENT"));
        stats.put("researcherCount", userRepository.countByRoleName("RESEARCHER"));
        stats.put("technicianCount", userRepository.countByRoleName("TECHNICIAN"));
        stats.put("activeUsers", userRepository.countByEnabledTrue());

        // JVM Memory Info
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;

        stats.put("totalMemory", totalMemory + " MB");
        stats.put("usedMemory", usedMemory + " MB");
        stats.put("freeMemory", freeMemory + " MB");
        stats.put("availableProcessors", runtime.availableProcessors());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/audit/export")
    public ResponseEntity<byte[]> exportAuditLogs() {
        // This will be implemented using AuditLogRepository
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Timestamp,Action,Resource Type,Resource ID,User ID,Username,IP Address\n");

        // For now, return empty CSV structure
        byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "audit_logs_" + LocalDate.now() + ".csv");
        headers.setContentLength(csvBytes.length);

        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }
}