package de.telekom.bot.service;

import de.telekom.bot.model.BeachLocation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class GeocodeServiceTest {
    
    @Test
    public void testGeocodeServiceCreation() {
        de.telekom.bot.config.ApiConfigurationProperties config = 
            new de.telekom.bot.config.ApiConfigurationProperties();
        de.telekom.bot.config.AppFeaturesConfig appFeaturesConfig = 
            new de.telekom.bot.config.AppFeaturesConfig();
        GeocodeService geocodeService = new GeocodeService(config, appFeaturesConfig);
        assertNotNull(geocodeService);
    }
    
    @Test
    public void testFindBeachCoordinates() {
        // Create configuration with default values
        de.telekom.bot.config.ApiConfigurationProperties config = 
            new de.telekom.bot.config.ApiConfigurationProperties();
        de.telekom.bot.config.AppFeaturesConfig appFeaturesConfig = 
            new de.telekom.bot.config.AppFeaturesConfig();
        
        GeocodeService geocodeService = new GeocodeService(config, appFeaturesConfig);
        
        // Test with a well-known beach - Benidorm
        BeachLocation location = geocodeService.findBeachCoordinates("Benidorm");
        
        // The result should not be null
        assertNotNull(location);
        
        System.out.println("=== Test Result for Benidorm ===");
        System.out.println("Found: " + location.isFound());
        
        if (location.isFound()) {
            System.out.println("Display Name: " + location.getDisplayName());
            System.out.println("Coordinates: " + location.getLatitude() + ", " + location.getLongitude());
            System.out.println("Type: " + location.getType());
            System.out.println("Classification: " + location.getClassification());
            System.out.println("Is Beach: " + location.isBeach());
            
            // Verify coordinates are reasonable for Spain
            assertTrue(location.getLatitude() > 35 && location.getLatitude() < 45, 
                    "Latitude should be within Spain's range: " + location.getLatitude());
            assertTrue(location.getLongitude() > -10 && location.getLongitude() < 5, 
                    "Longitude should be within Spain's range: " + location.getLongitude());
        } else {
            System.out.println("Location not found - this might indicate an API issue");
        }
        
        System.out.println("================================");
    }
}