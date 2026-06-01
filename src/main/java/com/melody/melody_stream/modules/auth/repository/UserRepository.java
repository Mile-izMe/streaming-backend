package com.melody.melody_stream.modules.auth.repository;

import com.melody.melody_stream.modules.auth.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByVerificationToken(String token);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    // Fetch users with roles + permissions in one query (avoid N + 1)
    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles ur
        LEFT JOIN FETCH ur.role r
        LEFT JOIN FETCH r.permissions rp
        LEFT JOIN FETCH rp.permission
        WHERE u.id = :userId AND u.deletedAt IS NULL
    """)
    Optional<User> findByIdWithRolesAndPermissions(@Param("userId") String userId);

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.roles ur
        LEFT JOIN FETCH ur.role r
        LEFT JOIN FETCH r.permissions rp
        LEFT JOIN FETCH rp.permission
        WHERE u.email = :email AND u.deletedAt IS NULL
    """)
    Optional<User> findByEmailWithRolesAndPermissions(@Param("email") String email);
}
