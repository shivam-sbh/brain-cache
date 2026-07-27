package com.braincache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Typed config for the spaced-repetition schedule. `braincache.review.*` from
 * application.yml (env-var backed) binds into this immutable record.
 *
 * intervalsDays is the ladder: a card sits at some index; a pass moves it one rung
 * up (capped at the last), a fail drops it back to index 0. emailFrom is the sender
 * on the daily review email. (email-cron is read directly by @Scheduled, so it's not
 * needed here.)
 */
@ConfigurationProperties(prefix = "braincache.review")
public record ReviewProperties(List<Integer> intervalsDays, String emailFrom) {
}
