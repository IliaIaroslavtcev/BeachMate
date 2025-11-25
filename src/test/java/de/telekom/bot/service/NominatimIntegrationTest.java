package de.telekom.bot.service;

import de.telekom.bot.config.ApiConfigurationProperties;
import de.telekom.bot.model.BeachLocation;
import org.junit.jupiter.api.Test;

/**
 * Integration test for Nominatim API to verify our fixes work
 */
public class NominatimIntegrationTest {
    
    @Test
    public void testNominatimAPIIntegration() {
        System.out.println("=== Testing Nominatim API Integration ===");
        
        // Create configuration with default values
        ApiConfigurationProperties config = new ApiConfigurationProperties();
        GeocodeService geocodeService = new GeocodeService(config, new de.telekom.bot.config.AppFeaturesConfig());
        
        // Test cases based on your working example
        String[] testCases = {
            "Benidorm",
            "Barcelona", 
            "Marbella",
            "Valencia"
        };
        
        for (String testCase : testCases) {
            System.out.println("\n🔍 Testing: " + testCase);
            
            try {
                BeachLocation location = geocodeService.findBeachCoordinates(testCase);
                
                if (location.isFound()) {
                    System.out.println("✅ SUCCESS");
                    System.out.println("   Display Name: " + location.getDisplayName());
                    System.out.println("   Coordinates: " + 
                        String.format("%.6f, %.6f", location.getLatitude(), location.getLongitude()));
                    System.out.println("   Type: " + location.getType());
                    System.out.println("   Classification: " + location.getClassification());
                    System.out.println("   Is Beach: " + location.isBeach());
                } else {
                    System.out.println("❌ NOT FOUND");
                }
                
            } catch (Exception e) {
                System.out.println("🚨 ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("\n=== Integration Test Complete ===");
    }
}