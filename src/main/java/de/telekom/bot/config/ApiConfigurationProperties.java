package de.telekom.bot.config;

import de.telekom.bot.model.NominatimConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for external APIs
 */
@Component
@ConfigurationProperties(prefix = "")
@Data
public class ApiConfigurationProperties {

    private final NominatimConfig nominatim = new NominatimConfig();
}
