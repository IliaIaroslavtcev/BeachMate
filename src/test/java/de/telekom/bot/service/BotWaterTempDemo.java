package de.telekom.bot.service;

import de.telekom.bot.config.ApiConfigurationProperties;
import de.telekom.bot.config.AppFeaturesConfig;
import de.telekom.bot.handler.BeachNameHandler;
import de.telekom.bot.model.BeachLocation;
import de.telekom.bot.model.WeatherInfo;

/**
 * Demo showing complete beach search with water temperature
 */
public class BotWaterTempDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Complete Beach Bot Demo with Water Temperature ===");
        
        // Create services
        ApiConfigurationProperties apiConfig = new ApiConfigurationProperties();
        AppFeaturesConfig appFeaturesConfig = new AppFeaturesConfig();
        
        GeocodeService geocodeService = new GeocodeService(apiConfig, appFeaturesConfig);
        JellyfishService jellyfishService = new JellyfishService(appFeaturesConfig);
        WeatherService weatherService = new WeatherService(appFeaturesConfig, jellyfishService);
        BeachCharacteristicsService beachCharacteristicsService = new BeachCharacteristicsService(appFeaturesConfig);
        UserLanguageService userLanguageService = new UserLanguageService();
        BeachNameHandler beachHandler = new BeachNameHandler(geocodeService, weatherService, beachCharacteristicsService, userLanguageService);
        
        // Test beach searches
        testBeachSearch("Benidorm", geocodeService, weatherService);
        testBeachSearch("Valencia", geocodeService, weatherService);
        testBeachSearch("Playa de la Concha", geocodeService, weatherService);
    }
    
    private static void testBeachSearch(String beachName, GeocodeService geocodeService, 
                                      WeatherService weatherService) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🏖️ Searching for: " + beachName);
        System.out.println("=".repeat(60));
        
        // Step 1: Find beach coordinates
        BeachLocation beachLocation = geocodeService.findBeachCoordinates(beachName);
        
        if (!beachLocation.isFound()) {
            System.out.println("❌ Beach not found!");
            return;
        }
        
        System.out.println("✅ Beach found!");
        System.out.println("📍 Location: " + beachLocation.getDisplayName());
        System.out.println("🗺️ Coordinates: " + String.format("%.6f", beachLocation.getLatitude()) + 
                         ", " + String.format("%.6f", beachLocation.getLongitude()));
        System.out.println("🏖️ Is Beach: " + beachLocation.isBeach());
        
        // Step 2: Get weather information
        WeatherInfo weatherInfo = weatherService.getWeatherInfo(beachLocation);
        
        System.out.println("\n🌡️ Weather Information:");
        System.out.println("• Air Temperature: " + weatherInfo.getFormattedAirTemperature());
        System.out.println("• Water Temperature: " + weatherInfo.getFormattedWaterTemperature());
        System.out.println("• Water Temp Source: " + (weatherInfo.isWaterTempFound() ? "Real-time data" : "Estimated"));
        System.out.println("• Data Source: " + weatherInfo.getSource());
        
        if (weatherInfo.hasTemperatureData()) {
            System.out.println("🌈 Comfort: " + weatherInfo.getComfortDescription());
        }
        
        System.out.println("\n📱 This is what users would see in Telegram:");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Simplified bot message preview
        StringBuilder message = new StringBuilder();
        message.append("🏖️ Beach Information 🌊\n\n");
        message.append("📍 Location: ").append(beachLocation.getDisplayName()).append("\n\n");
        message.append("🗺️ Coordinates:\n");
        message.append("• Latitude: ").append(String.format("%.6f", beachLocation.getLatitude())).append("\n");
        message.append("• Longitude: ").append(String.format("%.6f", beachLocation.getLongitude())).append("\n\n");
        
        if (weatherInfo.hasTemperatureData()) {
            message.append("🌡️ Current Conditions:\n");
            if (weatherInfo.isAirTempFound()) {
                message.append("• Air Temperature: **").append(weatherInfo.getFormattedAirTemperature()).append("**\n");
            }
            if (weatherInfo.isWaterTempFound()) {
                message.append("• Water Temperature: **").append(weatherInfo.getFormattedWaterTemperature()).append("**\n");
            }
            message.append("\n🌈 Comfort Level: ").append(weatherInfo.getComfortDescription()).append("\n\n");
        }
        
        message.append("💡 Try typing another beach name or use /help for examples!");
        
        System.out.println(message.toString());
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}