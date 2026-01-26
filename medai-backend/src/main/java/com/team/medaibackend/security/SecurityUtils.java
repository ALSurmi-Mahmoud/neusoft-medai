package com.team.medaibackend.security;

import com.team.medaibackend.entity.User;
import com.team.medaibackend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.team.medaibackend.exception.UnauthorizedException;

import java.util.Optional;

/**
 * Utility class for security-related operations.
 * Provides methods to safely extract user information from JWT authentication.
 */
@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get the currently authenticated user from SecurityContext.
     *
     * @return Optional containing the User if authenticated, empty otherwise
     */
    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username);
    }

    /**
     * Get the currently authenticated user, throwing exception if not found.
     *
     * @return The authenticated User
     * @throws UnauthorizedException if user is not authenticated or not found
     */
    public User getCurrentUserOrThrow() {
        return getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));
    }

    /**
     * Get the ID of the currently authenticated user.
     *
     * @return Optional containing the user ID if authenticated
     */
    public Optional<Long> getCurrentUserId() {
        return getCurrentUser().map(User::getId);
    }

    /**
     * Get the username of the currently authenticated user.
     *
     * @return Optional containing the username if authenticated
     */
    public Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        return Optional.of(authentication.getName());
    }

    /**
     * Check if the current user has a specific role.
     *
     * @param roleName The role name to check (e.g., "ADMIN", "DOCTOR")
     * @return true if user has the role, false otherwise
     */
    public boolean hasRole(String roleName) {
        return getCurrentUser()
                .map(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName().equals(roleName)))
                .orElse(false);
    }

    /**
     * Check if the current user is an admin.
     *
     * @return true if user is admin, false otherwise
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Check if the current user is a doctor.
     *
     * @return true if user is doctor, false otherwise
     */
    public boolean isDoctor() {
        return hasRole("DOCTOR");
    }
}