package de.telekom.bot.service;

import de.telekom.bot.config.ApiConfigurationProperties;
import de.telekom.bot.model.BeachLocation;
import de.telekom.bot.model.WeatherInfo;

/**
 * Demo to test real weather API calls
 */
public class WeatherApiDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Weather API Demo ===");
        
        // Create weather service
        de.telekom.bot.config.AppFeaturesConfig appFeaturesConfig = new de.telekom.bot.config.AppFeaturesConfig();
        JellyfishService jellyfishService = new JellyfishService(appFeaturesConfig);
        WeatherService weatherService = new WeatherService(appFeaturesConfig, jellyfishService);
        
        // Create Benidorm location
        BeachLocation benidorm = new BeachLocation();
        benidorm.setName("Benidorm Beach");
        benidorm.setDisplayName("Benidorm Beach, Paseo Marítimo de Levante, Benidorm, Spain");
        benidorm.setLatitude(38.535517);
        benidorm.setLongitude(-0.128690);
        benidorm.setFound(true);
        
        System.out.println("Testing location: " + benidorm.getName());
        System.out.println("Coordinates: " + benidorm.getLatitude() + ", " + benidorm.getLongitude());
        System.out.println();
        
        try {
            // Test real API call
            System.out.println("--- Attempting Real API Call ---");
            WeatherInfo realWeather = weatherService.getWeatherInfo(benidorm);
            
            if (realWeather.hasTemperatureData()) {
                System.out.println("✅ Real API data retrieved!");
                printWeatherInfo(realWeather);
            } else {
                System.out.println("❌ Real API failed, no temperature data");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Real API call failed: " + e.getMessage());
        }
        
        System.out.println("\n--- Using Mock Data for Comparison ---");
        WeatherInfo mockWeather = weatherService.getMockWeatherInfo(benidorm);
        printWeatherInfo(mockWeather);
        
        System.out.println("\n=== Complete Bot Message Preview ===");
        System.out.println(formatCompleteBeachMessage(benidorm, mockWeather));
    }
    
    private static void printWeatherInfo(WeatherInfo weather) {
        System.out.println("Air Temperature: " + weather.getFormattedAirTemperature());
        System.out.println("Water Temperature: " + weather.getFormattedWaterTemperature());
        System.out.println("Description: " + weather.getDescription());
        System.out.println("Humidity: " + weather.getHumidity() + "%");
        System.out.println("Wind Speed: " + weather.getWindSpeed() + " m/s");
        System.out.println("Comfort: " + weather.getComfortDescription());
        System.out.println("Source: " + weather.getSource());
    }
    
    private static String formatCompleteBeachMessage(BeachLocation location, WeatherInfo weatherInfo) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("🏖️ *Beach Information* 🌊\n\n")
          .append("📍 **Location:** ").append(location.getDisplayName()).append("\n\n")
          .append("🗺️ **Coordinates:**\n")
          .append("• Latitude: `").append(String.format("%.6f", location.getLatitude())).append("`\n")
          .append("• Longitude: `").append(String.format("%.6f", location.getLongitude())).append("`\n\n")
          .append("✅ **Type:** Confirmed beach location\n\n");
        
        // Add weather information
        if (weatherInfo != null && weatherInfo.hasTemperatureData()) {
            sb.append("🌡️ **Current Conditions:**\n");
            
            if (weatherInfo.isAirTempFound()) {
                sb.append("• Air Temperature: **")
                  .append(weatherInfo.getFormattedAirTemperature())
                  .append("**\n");
            }
            
            if (weatherInfo.isWaterTempFound()) {
                sb.append("• Water Temperature: **")
                  .append(weatherInfo.getFormattedWaterTemperature())
                  .append("**\n");
            }
            
            if (weatherInfo.getDescription() != null) {
                sb.append("• Weather: ").append(weatherInfo.getDescription()).append("\n");
            }
            
            if (weatherInfo.getHumidity() != null) {
                sb.append("• Humidity: ").append(weatherInfo.getHumidity()).append("%\n");
            }
            
            if (weatherInfo.getWindSpeed() != null) {
                sb.append("• Wind: ").append(String.format("%.1f m/s", weatherInfo.getWindSpeed())).append("\n");
            }
            
            sb.append("\n")
              .append("🌈 **Comfort Level:** ")
              .append(weatherInfo.getComfortDescription())
              .append("\n\n");
              
            if (weatherInfo.getSource() != null) {
                sb.append("📊 *Data source: ").append(weatherInfo.getSource()).append("*\n\n");
            }
        }
        
        sb.append("💡 *Try typing another beach name or use /help for examples!*");
        
        return sb.toString();
    }
}