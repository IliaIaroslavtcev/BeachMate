package de.telekom.bot.service;

import de.telekom.bot.config.AppFeaturesConfig;
import de.telekom.bot.model.BeachLocation;
import de.telekom.bot.model.WeatherInfo;

/**
 * Test water temperature functionality with real API calls
 */
public class WaterTemperatureTest {
    
    public static void main(String[] args) {
        System.out.println("=== Water Temperature Test ===");
        
        AppFeaturesConfig appFeaturesConfig = new AppFeaturesConfig();
        JellyfishService jellyfishService = new JellyfishService(appFeaturesConfig);
        WeatherService weatherService = new WeatherService(appFeaturesConfig, jellyfishService);
        
        // Test with Benidorm (Spain - Mediterranean)
        testLocation(weatherService, "Benidorm, Spain", 38.535517, -0.128690);
        
        // Test with Valencia (Spain - Mediterranean)
        testLocation(weatherService, "Valencia, Spain", 39.4699, -0.3763);
        
        // Test with Miami (US - Atlantic)
        testLocation(weatherService, "Miami, FL", 25.7617, -80.1918);
        
        // Test with San Diego (US - Pacific)
        testLocation(weatherService, "San Diego, CA", 32.7157, -117.1611);
        
        // Test with random location for fallback
        testLocation(weatherService, "Random Location", 45.0, 10.0);
    }
    
    private static void testLocation(WeatherService weatherService, String locationName, 
                                   double latitude, double longitude) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("Testing: " + locationName);
        System.out.println("Coordinates: " + latitude + ", " + longitude);
        System.out.println("=".repeat(50));
        
        BeachLocation location = new BeachLocation();
        location.setName(locationName);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setFound(true);
        
        WeatherInfo weatherInfo = weatherService.getWeatherInfo(location);
        
        System.out.println("Results:");
        System.out.println("• Air Temperature: " + weatherInfo.getFormattedAirTemperature());
        System.out.println("• Water Temperature: " + weatherInfo.getFormattedWaterTemperature());
        System.out.println("• Water Temp Found: " + weatherInfo.isWaterTempFound());
        System.out.println("• Source: " + weatherInfo.getSource());
        
        if (weatherInfo.isWaterTempFound()) {
            System.out.println("✅ Water temperature data retrieved successfully");
        } else {
            System.out.println("❌ No water temperature data available");
        }
    }
}