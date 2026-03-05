package com.team.medaibackend.service;

import com.team.medaibackend.entity.Role;
import com.team.medaibackend.entity.User;
import com.team.medaibackend.repository.RoleRepository;
import com.team.medaibackend.repository.UserRepository;
import com.team.medaibackend.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;
    private final SystemActivityService activityService;
    private final AuditService auditService;

    public AdminUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            SecurityUtils securityUtils,
            SystemActivityService activityService,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityUtils = securityUtils;
        this.activityService = activityService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public User createUser(User user, Set<String> roleNames) {
        User currentAdmin = securityUtils.getCurrentUserOrThrow();

        // Check if username or email exists
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Assign roles
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            roles.add(role);
        }
        user.setRoles(roles);

        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);

        // Log activity
        activityService.logActivity(
                "USER_CREATED",
                "User created: " + user.getUsername(),
                currentAdmin,
                "success"
        );

        auditService.log("CREATE", "USER", saved.getId().toString(),
                currentAdmin.getId(), currentAdmin.getUsername());

        return saved;
    }

    @Transactional
    public User updateUser(Long userId, User updates, Set<String> roleNames) {
        User currentAdmin = securityUtils.getCurrentUserOrThrow();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update basic fields
        if (updates.getFullName() != null) {
            user.setFullName(updates.getFullName());
        }
        if (updates.getEmail() != null) {
            user.setEmail(updates.getEmail());
        }
        if (updates.getPhone() != null) {
            user.setPhone(updates.getPhone());
        }

        // Update roles if provided
        if (roleNames != null && !roleNames.isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String roleName : roleNames) {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
                roles.add(role);
            }
            user.setRoles(roles);
        }

        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);

        activityService.logActivity(
                "USER_UPDATED",
                "User updated: " + user.getUsername(),
                currentAdmin,
                "success"
        );

        return saved;
    }

    @Transactional
    public void deleteUser(Long userId) {
        User currentAdmin = securityUtils.getCurrentUserOrThrow();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Don't allow deleting yourself
        if (user.getId().equals(currentAdmin.getId())) {
            throw new RuntimeException("Cannot delete your own account");
        }

        userRepository.delete(user);

        activityService.logActivity(
                "USER_DELETED",
                "User deleted: " + user.getUsername(),
                currentAdmin,
                "success"
        );
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User currentAdmin = securityUtils.getCurrentUserOrThrow();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        activityService.logActivity(
                "PASSWORD_RESET",
                "Password reset for user: " + user.getUsername(),
                currentAdmin,
                "success"
        );
    }

    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Toggle enabled status (assuming you have this field)
        // user.setEnabled(!user.getEnabled());
        userRepository.save(user);
    }
}