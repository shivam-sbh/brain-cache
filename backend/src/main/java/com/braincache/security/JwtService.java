package com.braincache.security;

import com.braincache.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Signs + verifies JWTs. @Service marks it a container-managed bean; JwtProperties
 * is constructor-injected (Spring sees the single constructor and supplies the bean —
 * no @Autowired needed). Same idea as passing deps into a Go struct constructor.
 *
 * API here is jjwt 0.12.x (builder + parser fluent style).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlHours;

    public JwtService(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.ttlHours = props.ttlHours();
    }

    /** Issue a token whose subject is the user's email. */
    public String issue(String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttlHours, ChronoUnit.HOURS)))
                .signWith(key)   // alg (HS256) inferred from key length
                .compact();
    }

    /** Verify signature + expiry, return the subject (email). Throws if invalid. */
    public String verify(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
