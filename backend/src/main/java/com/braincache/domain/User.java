package com.braincache.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity == a Go model struct with gorm tags, but tags are annotations.
 * @Entity + @Table map this class to the `users` table; each field maps to a column.
 * Hibernate reads these annotations to generate SQL (and, with ddl-auto=update, the table).
 */
@Entity
@Table(name = "users")
public class User {

    // IDENTITY == Postgres SERIAL/identity column; DB assigns the id on insert.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // bcrypt hash — never the raw password.
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // JPA needs a no-arg constructor (like a zero-value struct).
    protected User() {
    }

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
