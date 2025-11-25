package de.telekom.bot.model;

import lombok.Data;

/**
 * Configuration model for Nominatim API settings
 */
@Data
public class NominatimConfig {
    private final Api api = new Api();

    @Data
    public static class Api {
        private String baseUrl = "https://nominatim.openstreetmap.org";
        private String searchEndpoint = "/search";
        private String userAgent = "SpanishBeachBot/1.0";
        private int timeoutSeconds = 30;
        private int connectTimeoutSeconds = 10;
        private int limit = 3;
        private int rateLimitMs = 1000; // Minimum milliseconds between requests
        private String countryCodes = "es"; // ISO 3166-1alpha2 country codes (comma-separated)
        private boolean enabled = true;
    }
}
