package de.telekom.bot.service;

import de.telekom.bot.config.AppFeaturesConfig;
import de.telekom.bot.model.BeachLocation;
import de.telekom.bot.model.WeatherInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

/**
 * Test for WeatherService functionality
 */
public class WeatherServiceTest {
    
    private WeatherService weatherService;
    
    @Mock
    private JellyfishService jellyfishService;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        AppFeaturesConfig appFeaturesConfig = new AppFeaturesConfig();
        weatherService = new WeatherService(appFeaturesConfig, jellyfishService);
    }
    
    @Test
    public void testMockWeatherForBenidorm() {
        System.out.println("=== Testing Mock Weather Data ===");
        
        // Create a mock Benidorm location
        BeachLocation benidorm = new BeachLocation();
        benidorm.setName("Benidorm Beach");
        benidorm.setDisplayName("Benidorm Beach, Costa Blanca, Spain");
        benidorm.setLatitude(38.535517);
        benidorm.setLongitude(-0.128690);
        benidorm.setFound(true);
        
        // Get mock weather data
        WeatherInfo weather = weatherService.getMockWeatherInfo(benidorm);
        
        System.out.println("Location: " + weather.getLocation());
        System.out.println("Air Temperature: " + weather.getFormattedAirTemperature());
        System.out.println("Water Temperature: " + weather.getFormattedWaterTemperature());
        System.out.println("Description: " + weather.getDescription());
        System.out.println("Humidity: " + weather.getHumidity() + "%");
        System.out.println("Wind Speed: " + weather.getWindSpeed() + " m/s");
        System.out.println("Comfort Description: " + weather.getComfortDescription());
        System.out.println("Source: " + weather.getSource());
        
        System.out.println("\n=== Formatted Bot Message Preview ===");
        System.out.println(formatWeatherForBot(weather));
    }
    
    @Test 
    public void testMockWeatherForValencia() {
        System.out.println("\n=== Testing Mock Weather Data for Valencia ===");
        
        // Create a mock Valencia location
        BeachLocation valencia = new BeachLocation();
        valencia.setName("Valencia Beach");
        valencia.setDisplayName("Valencia Beach, Costa del Azahar, Spain");
        valencia.setLatitude(39.4699);
        valencia.setLongitude(-0.3763);
        valencia.setFound(true);
        
        // Get mock weather data
        WeatherInfo weather = weatherService.getMockWeatherInfo(valencia);
        
        System.out.println("Location: " + weather.getLocation());
        System.out.println("Air Temperature: " + weather.getFormattedAirTemperature());
        System.out.println("Water Temperature: " + weather.getFormattedWaterTemperature());
        System.out.println("Comfort Description: " + weather.getComfortDescription());
        
        System.out.println("\n=== Formatted Bot Message Preview ===");
        System.out.println(formatWeatherForBot(weather));
    }
    
    /**
     * Format weather info as it would appear in the bot message
     */
    private String formatWeatherForBot(WeatherInfo weather) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("🌡️ **Current Conditions:**\n");
        
        if (weather.isAirTempFound()) {
            sb.append("• Air Temperature: **")
              .append(weather.getFormattedAirTemperature())
              .append("**\n");
        }
        
        if (weather.isWaterTempFound()) {
            sb.append("• Water Temperature: **")
              .append(weather.getFormattedWaterTemperature())
              .append("**\n");
        }
        
        if (weather.getDescription() != null) {
            sb.append("• Weather: ").append(weather.getDescription()).append("\n");
        }
        
        if (weather.getHumidity() != null) {
            sb.append("• Humidity: ").append(weather.getHumidity()).append("%\n");
        }
        
        if (weather.getWindSpeed() != null) {
            sb.append("• Wind: ").append(String.format("%.1f m/s", weather.getWindSpeed())).append("\n");
        }
        
        sb.append("\n")
          .append("🌈 **Comfort Level:** ")
          .append(weather.getComfortDescription())
          .append("\n");
        
        return sb.toString();
    }
}