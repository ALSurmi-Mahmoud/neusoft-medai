package com.team.medaibackend;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String password = "password123";
        String hash = encoder.encode(password);
        
        System.out.println("Password: " + password);
        System.out.println("Generated hash: " + hash);
        System.out.println("Verification: " + encoder.matches(password, hash));
        
        // Test admin password
        String adminPassword = "admin123";
        String adminHash = encoder.encode(adminPassword);
        System.out.println("\nAdmin password: " + adminPassword);
        System.out.println("Generated hash: " + adminHash);
        System.out.println("Verification: " + encoder.matches(adminPassword, adminHash));
    }
}
