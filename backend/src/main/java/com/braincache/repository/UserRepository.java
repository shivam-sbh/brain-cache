package com.braincache.repository;

import com.braincache.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * The magic bit vs Go: you declare the INTERFACE only. Spring Data generates the
 * implementation at runtime (a proxy bean) — no hand-written SQL, no gorm calls.
 *
 * JpaRepository<User, Long> gives free CRUD: save, findById, findAll, delete...
 * `findByEmail` is a DERIVED query — Spring parses the method NAME into
 * `SELECT * FROM users WHERE email = ?`. Method name IS the query.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
