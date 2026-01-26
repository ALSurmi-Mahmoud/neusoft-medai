package com.team.medaibackend.web;

import com.team.medaibackend.entity.Role;
import com.team.medaibackend.entity.User;
import com.team.medaibackend.exception.UnauthorizedException;
import com.team.medaibackend.repository.AuditLogRepository;
import com.team.medaibackend.repository.RoleRepository;
import com.team.medaibackend.repository.UserRepository;
import com.team.medaibackend.security.SecurityUtils;
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
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    public AdminController(UserRepository userRepository,
                           RoleRepository roleRepository,
                           AuditLogRepository auditLogRepository,
                           AuditService auditService,
                           PasswordEncoder passwordEncoder,
                           SecurityUtils securityUtils) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled
    ) {
        // Verify admin access
        User currentUser = securityUtils.getCurrentUserOrThrow();
        if (!securityUtils.isAdmin()) {
            throw new UnauthorizedException("Admin access required");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> userPage;

        // Enhanced filtering logic
        if (search != null && !search.trim().isEmpty()) {
            userPage = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    search.trim(), search.trim(), pageable);
        } else if (role != null && !role.isEmpty()) {
            userPage = userRepository.findByRoleName(role, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        // Apply enabled filter if specified
        List<Map<String, Object>> userList = new ArrayList<>();
        for (User user : userPage.getContent()) {
            // Skip if enabled filter doesn't match
            if (enabled != null && !user.isEnabled() == enabled) {
                continue;
            }

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

            // Add flag if this is the current user
            userMap.put("isCurrentUser", user.getId().equals(currentUser.getId()));

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
        // Verify admin access
        if (!securityUtils.isAdmin()) {
            throw new UnauthorizedException("Admin access required");
        }

        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> request) {
        // Verify admin access
        User currentUser = securityUtils.getCurrentUserOrThrow();
        if (!securityUtils.isAdmin()) {
            throw new UnauthorizedException("Admin access required");
        }

        try {
            // Extract and validate fields
            String username = (String) request.get("username");
            String email = (String) request.get("email");
            String password = (String) request.get("password");
            String fullName = (String) request.get("fullName");
            String roleName = (String) request.get("role");

            // Validation
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Username is required"));
            }
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
            }
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid email format"));
            }
            if (password == null || password.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 6 characters"));
            }
            if (fullName == null || fullName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Full name is required"));
            }

            // Check for existing username/email
            if (userRepository.existsByUsername(username.trim())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Username already exists"));
            }
            if (userRepository.existsByEmail(email.trim().toLowerCase())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email already exists"));
            }

            // Create user
            User user = new User();
            user.setUsername(username.trim());
            user.setEmail(email.trim().toLowerCase());
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setFullName(fullName.trim());
            user.setPhone((String) request.get("phone"));
            user.setDepartment((String) request.get("department"));
            user.setTitle((String) request.get("title"));
            user.setEnabled(true);

            // Set role
            String roleToAssign = roleName != null ? roleName : "PATIENT";
            Role role = roleRepository.findByName(roleToAssign)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleToAssign));
            user.addRole(role);

            User saved = userRepository.save(user);

            // Audit log with actual user info
            auditService.log("CREATE_USER", "USER", saved.getId().toString(),
                    currentUser.getId(), currentUser.getUsername());

            return ResponseEntity.ok(Map.of(
                    "message", "User created successfully",
                    "id", saved.getId(),
                    "username", saved.getUsername()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        // Verify admin access
        User currentUser = securityUtils.getCurrentUserOrThrow();
        if (!securityUtils.isAdmin()) {
            throw new UnauthorizedException("Admin access required");
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();

        try {
            // Update basic fields
            if (request.containsKey("fullName")) {
                String fullName = (String) request.get("fullName");
                if (fullName == null || fullName.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Full name cannot be empty"));
                }
                user.setFullName(fullName.trim());
            }

            if (request.containsKey("email")) {
                String email = (String) request.get("email");
                if (email == null || email.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Email cannot be empty"));
                }
                if (!EMAIL_PATTERN.matcher(email).matches()) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Invalid email format"));
                }
                // Check if email is already used by another user
                Optional<User> existingUser = userRepository.findByEmail(email.trim().toLowerCase());
                if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Email already in use"));
                }
                user.setEmail(email.trim().toLowerCase());
            }

            if (request.containsKey("phone")) user.setPhone((String) request.get("phone"));
            if (request.containsKey("department")) user.setDepartment((String) request.get("department"));
            if (request.containsKey("title")) user.setTitle((String) request.get("title"));

            // Handle role change
            if (request.containsKey("role")) {
                String newRoleName = (String) request.get("role");

                // Prevent admin from removing their own admin role
                if (user.getId().equals(currentUser.getId()) &&
                        "ADMIN".equals(user.getPrimaryRole()) &&
                        !"ADMIN".equals(newRoleName)) {
                    return ResponseEntity.badRequest().body(
                            Map.of("message", "You cannot remove your own admin role")
                    );
                }

                user.getRoles().clear();
                Role role = roleRepository.findByName(newRoleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + newRoleName));
                user.addRole(role);
            }

            userRepository.save(user);

            // Audit log
            auditService.log("UPDATE_USER", "USER", id.toString(),
                    currentUser.getId(), currentUser.getUsername());

            return ResponseEntity.ok(Map.of("message", "User updated successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        // Verify admin access
        User currentUser = securityUtils.getCurrentUserOrThrow();
        if (!securityUtils.isAdmin()) {
            throw new UnauthorizedException("Admin access required");
        }

        Boolean enabled = (Boolean) request.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "enabled field is required"));
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();

        // Prevent admin from disabling themselves
        if (user.getId().equals(currentUser.getId()) && !enabled) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "You cannot disable your own account")
            );
        }

        // Prevent disabling the last admin
        if (!enabled && "ADMIN".equals(user.getPrimaryRole())) {
            long adminCount = userRepository.countByRoleName("ADMIN");
            long enabledAdminCount = userRepository.findByRoleNameOrderByFullNameAsc("ADMIN")
                    .stream()
                    .filter(User::isEnabled)
                    .count();

            if (enabledAdminCount <= 1) {
                return ResponseEntity.badRequest().body(
                        Map.of("message", "Cannot disable the last active admin")
                );
            }
        }

        user.setEnabled(enabled);
        userRepository.save(user);

        // Audit log
        auditService.log(enabled ? "ENABLE_USER" : "DISABLE_USER", "USER", id.toString(),
                currentUser.getId(), currentUser.getUsername());

        return ResponseEntity.ok(Map.of(
                "message", "User " + (enabled ? "enabled" : "disabled") + " successfully"
        ));
    }

    @PutMapping("/users/{id}/password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        // Verify admin access
        User currentUser = securityUtils.getCurrentUserOrThrow();
        if (!securityUtils.isAdmin()) {
            throw new UnauthorizedException("Admin access required");
        }

        String newPassword = request.get("password");

        // Validation
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Password must be at least 6 characters")
            );
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Audit log
        auditService.log("RESET_PASSWORD", "USER", id.toString(),
                currentUser.getId(), currentUser.getUsername());

        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        // Verify admin access
        User currentUser = securityUtils.getCurrentUserOrThrow();
        if (!securityUtils.isAdmin()) {
            throw new UnauthorizedException("Admin access required");
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();

        // Prevent self-deletion
        if (user.getId().equals(currentUser.getId())) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "You cannot delete your own account")
            );
        }

        // Prevent deleting the last admin
        if ("ADMIN".equals(user.getPrimaryRole())) {
            long adminCount = userRepository.countByRoleName("ADMIN");
            if (adminCount <= 1) {
                return ResponseEntity.badRequest().body(
                        Map.of("message", "Cannot delete the last admin user")
                );
            }
        }

        String deletedUsername = user.getUsername();

        // Nullify user references in audit logs (preserves audit history)
        auditLogRepository.nullifyUserReferences(id);

        // Log the deletion BEFORE actually deleting
        auditService.log("DELETE_USER", "USER", id.toString(),
                currentUser.getId(), currentUser.getUsername());

        // Delete the user
        userRepository.deleteById(id);

        return ResponseEntity.ok(Map.of(
                "message", "User '" + deletedUsername + "' deleted successfully",
                "deletedId", id
        ));
    }

    @PostMapping("/users/bulk-status")
    public ResponseEntity<?> bulkUpdateStatus(@RequestBody Map<String, Object> request) {
        // Verify admin access
        User currentUser = securityUtils.getCurrentUserOrThrow();
        if (!securityUtils.isAdmin()) {
            throw new UnauthorizedException("Admin access required");
        }

        @SuppressWarnings("unchecked")
        List<Long> userIds = (List<Long>) request.get("userIds");
        Boolean enabled = (Boolean) request.get("enabled");

        if (userIds == null || userIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No users selected"));
        }
        if (enabled == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "enabled field is required"));
        }

        int updated = 0;
        List<String> errors = new ArrayList<>();

        for (Long userId : userIds) {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                errors.add("User " + userId + " not found");
                continue;
            }

            User user = userOpt.get();

            // Skip if trying to disable self
            if (user.getId().equals(currentUser.getId()) && !enabled) {
                errors.add("Cannot disable your own account");
                continue;
            }

            user.setEnabled(enabled);
            userRepository.save(user);
            updated++;

            auditService.log(enabled ? "ENABLE_USER" : "DISABLE_USER", "USER", userId.toString(),
                    currentUser.getId(), currentUser.getUsername());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", updated + " users updated");
        response.put("updated", updated);
        if (!errors.isEmpty()) {
            response.put("errors", errors);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        // Verify admin access
        if (!securityUtils.isAdmin()) {
            throw new UnauthorizedException("Admin access required");
        }

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
        // Verify admin access
        if (!securityUtils.isAdmin()) {
            throw new UnauthorizedException("Admin access required");
        }

        // Simple CSV export
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