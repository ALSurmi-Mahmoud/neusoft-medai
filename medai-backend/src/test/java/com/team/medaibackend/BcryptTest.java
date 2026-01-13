package com.team.medaibackend;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptTest {
    
    @Test
    public void testBcryptMatching() {
        // Test with default strength (10 rounds)
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String plainPassword = "password123";
        String hashFromDB = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";
        
        System.out.println("=== BCrypt Test ===");
        System.out.println("Plain password: " + plainPassword);
        System.out.println("Hash from DB: " + hashFromDB);
        
        boolean matches = encoder.matches(plainPassword, hashFromDB);
        System.out.println("DEFAULT encoder matches: " + matches);
        
        if (!matches) {
            // Try with different BCrypt versions
            System.out.println("\nTrying to generate new hash...");
            String newHash = encoder.encode(plainPassword);
            System.out.println("New hash: " + newHash);
            System.out.println("New hash matches: " + encoder.matches(plainPassword, newHash));
            
            // Test if the hash is even valid
            try {
                System.out.println("\nTesting hash validity...");
                encoder.matches("test", hashFromDB);
                System.out.println("Hash is valid format");
            } catch (Exception e) {
                System.out.println("Hash is INVALID: " + e.getMessage());
            }
        }
    }
}
