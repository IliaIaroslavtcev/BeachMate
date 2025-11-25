package de.telekom.bot.service;

import de.telekom.bot.config.ApiConfigurationProperties;
import de.telekom.bot.model.BeachLocation;

/**
 * Simple main class to test beach search functionality
 */
public class BeachSearchMain {
    
    public static void main(String[] args) {
        System.out.println("=== Testing Improved Beach Search ===");
        
        // Create service
        ApiConfigurationProperties config = new ApiConfigurationProperties();
        de.telekom.bot.config.AppFeaturesConfig appFeaturesConfig = new de.telekom.bot.config.AppFeaturesConfig();
        GeocodeService geocodeService = new GeocodeService(config, appFeaturesConfig);
        
        String[] testQueries = {
            "benidorm",      // Should become "benidorm beach"
            "BENIDORM",      // Case insensitive test  
            "Benidorm",      // Mixed case
            "playa levante", // Already has beach keyword
            "valencia",      // Should become "valencia beach"
            "alicante"       // Should become "alicante beach"
        };
        
        for (String query : testQueries) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("Testing query: '" + query + "'");
            System.out.println("=".repeat(50));
            
            try {
                BeachLocation location = geocodeService.findBeachCoordinates(query);
                
                if (location.isFound()) {
                    System.out.println("✅ FOUND!");
                    System.out.println("Name: " + location.getName());
                    System.out.println("Display Name: " + location.getDisplayName());
                    System.out.println("Coordinates: " + String.format("%.6f", location.getLatitude()) + 
                                     ", " + String.format("%.6f", location.getLongitude()));
                    System.out.println("Type: " + location.getType());
                    System.out.println("Classification: " + location.getClassification());
                    System.out.println("Is Beach: " + location.isBeach());
                    System.out.println("Importance: " + location.getImportance());
                } else {
                    System.out.println("❌ NOT FOUND");
                }
                
                // Add delay to respect API limits
                Thread.sleep(2000);
                
            } catch (Exception e) {
                System.out.println("❌ ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("\n=== Test completed ===");
    }
}