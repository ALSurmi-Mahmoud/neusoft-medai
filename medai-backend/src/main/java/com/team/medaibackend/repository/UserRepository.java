package com.team.medaibackend.repository;

import com.team.medaibackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // Role queries - using @Query to join through the roles relationship
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :role ORDER BY u.fullName ASC")
    List<User> findByRoleNameOrderByFullNameAsc(@Param("role") String role);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.name = :role")
    long countByRoleName(@Param("role") String role);

    long countByEnabledTrue();

    // Pagination and search
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :role")
    Page<User> findByRoleName(@Param("role") String role, Pageable pageable);

    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String email, Pageable pageable);
}