package com.braincache.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @Configuration class == a place where you hand-build beans the container can't
 * auto-create. Each @Bean method's return value becomes a singleton in the container,
 * injectable anywhere by type. Think of it as explicit wire.Build providers.
 *
 * @EnableConfigurationProperties turns JwtProperties into an injectable bean.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    // One bcrypt encoder for the whole app. Inject wherever we hash/verify passwords.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
