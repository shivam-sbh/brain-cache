package com.braincache.service;

import com.braincache.domain.User;
import com.braincache.repository.UserRepository;
import com.braincache.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic layer — the "service struct with methods" you'd write in Go.
 * Deliberately gRPC-agnostic: knows nothing about proto types. Keeps transport
 * (gRPC) and domain logic separate so either can change independently.
 *
 * All three deps constructor-injected by the container.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwt) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    /** Result carrier so the service returns domain data, not proto. */
    public record AuthResult(String token, String email) {
    }

    // @Transactional: wraps the method in a DB transaction (commit on return,
    // rollback on runtime exception). Like defer tx.Rollback()/tx.Commit(), automatic.
    @Transactional
    public AuthResult register(String email, String rawPassword) {
        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException("email already registered");
        }
        String hash = passwordEncoder.encode(rawPassword);
        User saved = users.save(new User(email, hash));
        return new AuthResult(jwt.issue(saved.getEmail()), saved.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResult login(String email, String rawPassword) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("invalid credentials"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("invalid credentials");
        }
        return new AuthResult(jwt.issue(user.getEmail()), user.getEmail());
    }
}
