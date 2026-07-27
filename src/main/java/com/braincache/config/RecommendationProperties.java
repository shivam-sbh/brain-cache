package com.braincache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Typed config for the daily DSA recommendation. `braincache.recommendation.*` from
 * application.yml (env-var backed).
 *
 * hard/medium/easy are 7-slot weekly schedules (Mon..Sun): each slot is how many
 * problems of that difficulty to recommend that weekday. 0 — or a slot past the end of
 * a short array — means none. dataDir points at the folder holding the CSV export.
 * (cron is read directly by @Scheduled.)
 */
@ConfigurationProperties(prefix = "braincache.recommendation")
public record RecommendationProperties(
        List<Integer> hard,
        List<Integer> medium,
        List<Integer> easy,
        String dataDir) {

    /** Count for a difficulty on a given weekday index (0 = Monday). Null/short-safe. */
    public int countFor(List<Integer> schedule, int dayIndex) {
        if (schedule == null || dayIndex < 0 || dayIndex >= schedule.size()) {
            return 0;
        }
        Integer n = schedule.get(dayIndex);
        return n == null ? 0 : n;
    }
}
