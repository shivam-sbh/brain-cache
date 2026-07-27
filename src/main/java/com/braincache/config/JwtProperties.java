package com.braincache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed config binding. `braincache.jwt.*` from application.yml (which itself pulls
 * from env vars) binds into this immutable record. Like unmarshalling a config
 * struct in Go, but Spring does it and hands you a ready bean.
 */
@ConfigurationProperties(prefix = "braincache.jwt")
public record JwtProperties(String secret, long ttlHours) {
}
