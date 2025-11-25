package de.telekom.bot.service;

import de.telekom.bot.config.ApiConfigurationProperties;
import de.telekom.bot.model.BeachLocation;

/**
 * Simple test runner to verify Nominatim API integration works correctly
 */
public class NominatimTestRunner {
    
    public static void main(String[] args) {
        // Create configuration manually
        ApiConfigurationProperties config = new ApiConfigurationProperties();
        
        // Create service
        GeocodeService geocodeService = new GeocodeService(config, new de.telekom.bot.config.AppFeaturesConfig());
        
        // Test beaches
        String[] testBeaches = {"Benidorm", "Playa de la Concha", "Marbella", "Barcelona", "Costa Brava"};
        
        System.out.println("=== Testing Nominatim API Integration ===");
        
        for (String beachName : testBeaches) {
            System.out.println("\n🔍 Testing: " + beachName);
            
            BeachLocation location = geocodeService.findBeachCoordinates(beachName);
            
            if (location.isFound()) {
                System.out.println("✅ Found: " + location.getDisplayName());
                System.out.println("   Coordinates: " + location.getLatitude() + ", " + location.getLongitude());
                System.out.println("   Type: " + location.getType());
                System.out.println("   Is Beach: " + location.isBeach());
            } else {
                System.out.println("❌ Not found");
            }
        }
        
        System.out.println("\n=== Test Complete ===");
    }
}