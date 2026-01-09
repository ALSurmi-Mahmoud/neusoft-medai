package com.team.medaibackend.service;

import com.team.medaibackend.dto.LoginRequest;
import com.team.medaibackend.dto.LoginResponse;
import com.team.medaibackend.dto.RegisterRequest;
import com.team.medaibackend.entity.Role;
import com.team.medaibackend.entity.User;
import com.team.medaibackend.repository.RoleRepository;
import com.team.medaibackend.repository.UserRepository;
import com.team.medaibackend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        if (!user.getEnabled()) {
            throw new RuntimeException("Account is disabled");
        }

        if (user.getAccountLocked() != null && user.getAccountLocked()) {
            throw new RuntimeException("Account is locked");
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getPrimaryRole(), user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        // Audit log
        auditService.log(AuditService.ACTION_LOGIN, "USER", user.getId().toString(),
                user.getId(), user.getUsername());

        return buildLoginResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email already exists
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setDepartment(request.getDepartment());
        user.setTitle(request.getTitle());
        user.setSpecialization(request.getSpecialization());
        user.setLicenseNumber(request.getLicenseNumber());
        user.setEnabled(true);
        user.setAccountLocked(false);

        // Assign role
        String roleName = request.getRole() != null ? request.getRole().toUpperCase() : "PATIENT";
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(roleName);
                    return roleRepository.save(newRole);
                });

        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(savedUser.getUsername(), savedUser.getPrimaryRole(), savedUser.getId());
        String refreshToken = jwtService.generateRefreshToken(savedUser.getUsername());

        // Audit log
        auditService.log(AuditService.ACTION_CREATE, "USER", savedUser.getId().toString(),
                savedUser.getId(), savedUser.getUsername());

        return buildLoginResponse(savedUser, accessToken, refreshToken);
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String username = jwtService.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = jwtService.generateAccessToken(username, user.getPrimaryRole(), user.getId());

        return buildLoginResponse(user, newAccessToken, refreshToken);
    }

    private LoginResponse buildLoginResponse(User user, String accessToken, String refreshToken) {
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(86400L);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setFullName(user.getFullName());
        userInfo.setRole(user.getPrimaryRole());
        userInfo.setDepartment(user.getDepartment());
        userInfo.setTitle(user.getTitle());

        response.setUser(userInfo);
        return response;
    }
}