package de.telekom.bot.util;

import de.telekom.bot.config.ApiConfigurationProperties;
import de.telekom.bot.config.AppFeaturesConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Logs configuration settings when the application starts
 */
@Component
@RequiredArgsConstructor
public class ConfigurationLogger {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationLogger.class);

    private final ApiConfigurationProperties apiConfig;
    private final AppFeaturesConfig appFeaturesConfig;

    @EventListener(ApplicationReadyEvent.class)
    public void logConfiguration() {
        logger.info("=== Spanish Beach Bot Configuration ===");

        // Log Nominatim configuration
        var nominatim = apiConfig.getNominatim().getApi();
        logger.info("📍 Nominatim OSM API:");
        logger.info("   Base URL: {}", nominatim.getBaseUrl());
        logger.info("   Enabled: {}", nominatim.isEnabled());
        logger.info("   Timeout: {}s", nominatim.getTimeoutSeconds());
        logger.info("   Limit: {} results", nominatim.getLimit());

        // Log Feature flags
        var features = appFeaturesConfig.getFeatures();
        logger.info("🎯 Features:");
        logger.info("   Geocoding: {}", features.isGeocoding());
        logger.info("   Weather: {}", features.isWeather());

        logger.info("=======================================");
    }
}