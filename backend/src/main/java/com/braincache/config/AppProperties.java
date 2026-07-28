package com.braincache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Top-level app config. appUrl is the frontend base URL used for the CTA button in
 * outbound email; blank by default (local/dev), set only in prod via APP_URL. Jobs
 * check it themselves — a blank or non-http value just means no button renders.
 */
@ConfigurationProperties(prefix = "braincache")
public record AppProperties(String appUrl) {

    public boolean hasValidAppUrl() {
        return appUrl != null && (appUrl.startsWith("http://") || appUrl.startsWith("https://"));
    }
}
