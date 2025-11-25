package de.telekom.bot.service;

import de.telekom.bot.config.ApiConfigurationProperties;
import de.telekom.bot.model.BeachLocation;
import org.junit.jupiter.api.Test;

/**
 * Simple test to verify improved beach search functionality
 */
public class BeachSearchTest {
    
    @Test
    public void testCaseInsensitiveBeachSearch() {
        System.out.println("=== Testing Case-Insensitive Beach Search ===");
        
        // Create service
        ApiConfigurationProperties config = new ApiConfigurationProperties();
        GeocodeService geocodeService = new GeocodeService(config, new de.telekom.bot.config.AppFeaturesConfig());
        
        String[] testQueries = {
            "benidorm",      // Should find Benidorm beaches
            "BENIDORM",      // Case insensitive
            "Benidorm",      // Mixed case
            "alicante",      // Another city
            "valencia"       // Another city
        };
        
        for (String query : testQueries) {
            System.out.println("\n--- Testing query: '" + query + "' ---");
            
            try {
                BeachLocation location = geocodeService.findBeachCoordinates(query);
                
                if (location.isFound()) {
                    System.out.println("✅ Found: " + location.getDisplayName());
                    System.out.println("   Coordinates: " + String.format("%.6f", location.getLatitude()) + 
                                     ", " + String.format("%.6f", location.getLongitude()));
                    System.out.println("   Type: " + location.getType());
                    System.out.println("   Classification: " + location.getClassification());
                    System.out.println("   Is Beach: " + location.isBeach());
                } else {
                    System.out.println("❌ Not found");
                }
                
                // Add delay to respect API limits
                Thread.sleep(1000);
                
            } catch (Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
            }
        }
        
        System.out.println("\n=== Test completed ===");
    }
}