package de.telekom.bot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for application features and toggles
 */
@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppFeaturesConfig {

    private final Features features = new Features();

    @Data
    public static class Features {
        private boolean geocoding = true;
        private boolean weather = true;
    }
}
