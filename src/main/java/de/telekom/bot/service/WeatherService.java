package de.telekom.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.telekom.bot.config.AppFeaturesConfig;
import de.telekom.bot.model.BeachLocation;
import de.telekom.bot.model.JellyfishInfo;
import de.telekom.bot.model.WeatherInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AppFeaturesConfig appFeaturesConfig;
    private final JellyfishService jellyfishService;

    /**
     * Get weather information for a beach location
     */
    public WeatherInfo getWeatherInfo(BeachLocation location) {
        if (location == null || !location.isFound()) {
            logger.warn("Cannot get weather for invalid location");
            return new WeatherInfo();
        }

        logger.info("Fetching weather info for location: {} at {}, {}",
                location.getName(), location.getLatitude(), location.getLongitude());

        WeatherInfo weatherInfo = new WeatherInfo();
        weatherInfo.setLocation(location.getName());

        // Get air temperature from Open-Meteo (free API, no key required)
        try {
            Double airTemp = getAirTemperatureFromOpenMeteo(location.getLatitude(), location.getLongitude());
            weatherInfo.setAirTemperature(airTemp);
            logger.info("Air temperature retrieved: {}°C", airTemp);
        } catch (Exception e) {
            logger.error("Failed to get air temperature", e);
        }

        // Get water temperature from multiple sources
        try {
            Double waterTemp = getWaterTemperature(location.getLatitude(), location.getLongitude());
            weatherInfo.setWaterTemperature(waterTemp);
            if (waterTemp != null) {
                logger.info("Water temperature retrieved: {}°C", waterTemp);
            } else {
                logger.warn("No water temperature data available for this location");
            }
        } catch (Exception e) {
            logger.error("Failed to get water temperature", e);
        }

        // Get jellyfish information if enabled
        try {
            if (jellyfishService.isEnabled()) {
                JellyfishInfo jellyfishInfo = jellyfishService.getJellyfishInfo(location);
                weatherInfo.setJellyfishInfo(jellyfishInfo);
                logger.info("Jellyfish risk level: {}", jellyfishInfo.getRiskLevel());
            }
        } catch (Exception e) {
            logger.error("Failed to get jellyfish information", e);
        }

        weatherInfo.setSource("Open-Meteo API + Marine Biology APIs");
        return weatherInfo;
    }

    /**
     * Get air temperature from Open-Meteo Weather API (free, no key required)
     */
    private Double getAirTemperatureFromOpenMeteo(double latitude, double longitude)
            throws IOException, InterruptedException {

        String url = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%.6f&longitude=%.6f&current_weather=true",
                latitude, longitude
        );

        logger.debug("Air temperature request URL: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Spanish Beach Bot/1.0")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode currentWeather = root.get("current_weather");

            if (currentWeather != null && currentWeather.has("temperature")) {
                return currentWeather.get("temperature").asDouble();
            }
        }

        logger.warn("Failed to parse air temperature from Open-Meteo response: {}", response.statusCode());
        return null;
    }

    /**
     * Get water temperature from multiple sources (tries different APIs)
     */
    private Double getWaterTemperature(double latitude, double longitude) {
        logger.debug("Attempting to get water temperature for {}, {}", latitude, longitude);

        // Try Open-Meteo Marine API first (most reliable for coastal areas)
        try {
            Double temp = getWaterTemperatureFromOpenMeteo(latitude, longitude);
            if (temp != null) {
                logger.debug("Got water temperature from Open-Meteo Marine: {}°C", temp);
                return temp;
            }
        } catch (Exception e) {
            logger.debug("Open-Meteo Marine API failed: {}", e.getMessage());
        }

        // Try OpenWeatherMap One Call API (if we had a key, but we can try the free tier)
        try {
            Double temp = getWaterTemperatureFromOpenWeatherFree(latitude, longitude);
            if (temp != null) {
                logger.debug("Got water temperature from OpenWeatherMap: {}°C", temp);
                return temp;
            }
        } catch (Exception e) {
            logger.debug("OpenWeatherMap API failed: {}", e.getMessage());
        }

        // Try NOAA if near US waters (limited but free)
        try {
            Double temp = getWaterTemperatureFromNOAA(latitude, longitude);
            if (temp != null) {
                logger.debug("Got water temperature from NOAA: {}°C", temp);
                return temp;
            }
        } catch (Exception e) {
            logger.debug("NOAA API failed: {}", e.getMessage());
        }

        // Last resort: estimate based on location and season
        Double estimatedTemp = estimateWaterTemperature(latitude, longitude);
        if (estimatedTemp != null) {
            logger.info("Using estimated water temperature: {}°C (no real-time data available)", estimatedTemp);
        }

        return estimatedTemp;
    }

    /**
     * Get water temperature from Open-Meteo Marine API
     */
    private Double getWaterTemperatureFromOpenMeteo(double latitude, double longitude)
            throws IOException, InterruptedException {

        String url = String.format(
                "https://marine-api.open-meteo.com/v1/marine?latitude=%.6f&longitude=%.6f&current=sea_surface_temperature",
                latitude, longitude
        );

        logger.debug("Water temperature request URL: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Spanish Beach Bot/1.0")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode current = root.get("current");

            if (current != null && current.has("sea_surface_temperature")) {
                JsonNode tempNode = current.get("sea_surface_temperature");
                if (!tempNode.isNull()) {
                    return tempNode.asDouble();
                }
            }
        }

        logger.warn("Failed to parse water temperature from Open-Meteo Marine response: {}", response.statusCode());
        return null;
    }

    /**
     * Try to get water temperature from OpenWeatherMap (limited free access)
     */
    private Double getWaterTemperatureFromOpenWeatherFree(double latitude, double longitude)
            throws IOException, InterruptedException {

        // OpenWeatherMap free tier doesn't include water temperature in the basic weather API
        // This is a placeholder for when they have a key or use the One Call API
        // For now, we'll return null to indicate no data available
        return null;
    }

    /**
     * Try to get water temperature from NOAA (US waters only, free but limited)
     */
    private Double getWaterTemperatureFromNOAA(double latitude, double longitude)
            throws IOException, InterruptedException {

        // NOAA only covers US waters, so check if coordinates are in reasonable US coastal range
        if ((latitude < 25 || latitude > 50) || (longitude < -130 || longitude > -65)) {
            return null; // Not in US waters
        }

        // NOAA API is complex and requires station IDs, so for now return null
        // Could be implemented with station lookup in the future
        return null;
    }

    /**
     * Estimate water temperature based on location, season, and typical patterns
     */
    private Double estimateWaterTemperature(double latitude, double longitude) {
        java.time.LocalDate now = java.time.LocalDate.now();
        int dayOfYear = now.getDayOfYear();
        int month = now.getMonthValue();

        // Mediterranean Sea (Spain, France, Italy, etc.)
        if (latitude >= 30 && latitude <= 45 && longitude >= -6 && longitude <= 36) {
            return estimateMediterraneanWaterTemp(latitude, month);
        }

        // Atlantic Ocean - European coast
        if (latitude >= 35 && latitude <= 60 && longitude >= -15 && longitude <= -5) {
            return estimateAtlanticEuropeWaterTemp(latitude, month);
        }

        // Atlantic Ocean - US East Coast
        if (latitude >= 25 && latitude <= 45 && longitude >= -85 && longitude <= -65) {
            return estimateAtlanticUSWaterTemp(latitude, month);
        }

        // Pacific Ocean - US West Coast
        if (latitude >= 30 && latitude <= 50 && longitude >= -130 && longitude <= -115) {
            return estimatePacificUSWaterTemp(latitude, month);
        }

        // Gulf of Mexico
        if (latitude >= 25 && latitude <= 30 && longitude >= -100 && longitude <= -80) {
            return estimateGulfMexicoWaterTemp(month);
        }

        // Default: rough global estimate
        return estimateGlobalWaterTemp(latitude, month);
    }

    private Double estimateMediterraneanWaterTemp(double latitude, int month) {
        // Mediterranean is warmer in summer, cooler in winter
        // Northern Med is cooler than Southern Med
        double baseTempSouth = 20.0; // Southern Med average
        double baseTempNorth = 16.0; // Northern Med average

        double baseTemp = baseTempSouth - ((latitude - 35) / 10.0) * (baseTempSouth - baseTempNorth);

        // Seasonal variation: peak in August-September, minimum in February-March
        double[] monthlyOffset = {-4, -5, -4, -2, 1, 4, 6, 8, 7, 4, 0, -2};

        return Math.round((baseTemp + monthlyOffset[month - 1]) * 10.0) / 10.0;
    }

    private Double estimateAtlanticEuropeWaterTemp(double latitude, int month) {
        // Atlantic is generally cooler than Mediterranean
        double baseTemp = 14.0 - ((latitude - 40) / 10.0) * 3.0; // Cooler as you go north

        double[] monthlyOffset = {-3, -4, -3, -1, 2, 4, 6, 6, 4, 2, -1, -2};

        return Math.round((baseTemp + monthlyOffset[month - 1]) * 10.0) / 10.0;
    }

    private Double estimateAtlanticUSWaterTemp(double latitude, int month) {
        // US East Coast varies significantly by latitude
        double baseTemp = 18.0 - ((latitude - 35) / 10.0) * 6.0;

        double[] monthlyOffset = {-6, -7, -5, -2, 2, 5, 7, 8, 6, 2, -2, -4};

        return Math.round((baseTemp + monthlyOffset[month - 1]) * 10.0) / 10.0;
    }

    private Double estimatePacificUSWaterTemp(double latitude, int month) {
        // Pacific US coast is moderated by currents, generally cooler
        double baseTemp = 15.0 - ((latitude - 35) / 15.0) * 4.0;

        double[] monthlyOffset = {-2, -2, -1, 0, 1, 2, 3, 3, 2, 1, 0, -1};

        return Math.round((baseTemp + monthlyOffset[month - 1]) * 10.0) / 10.0;
    }

    private Double estimateGulfMexicoWaterTemp(int month) {
        // Gulf of Mexico is generally warm
        double baseTemp = 26.0;

        double[] monthlyOffset = {-4, -3, -2, 0, 2, 3, 3, 3, 2, 0, -2, -3};

        return Math.round((baseTemp + monthlyOffset[month - 1]) * 10.0) / 10.0;
    }

    private Double estimateGlobalWaterTemp(double latitude, int month) {
        // Very rough global estimate based on latitude
        double absLatitude = Math.abs(latitude);
        double baseTemp = 25.0 - (absLatitude / 90.0) * 20.0; // Warmer at equator, colder at poles

        // Simple seasonal variation (more pronounced away from equator)
        double seasonalFactor = absLatitude / 90.0; // 0 at equator, 1 at poles

        // Northern hemisphere seasons
        double[] monthlyOffset = {-3, -4, -2, 0, 3, 5, 6, 6, 4, 1, -1, -2};
        if (latitude < 0) { // Southern hemisphere - reverse seasons
            monthlyOffset = new double[]{6, 6, 4, 1, -1, -2, -3, -4, -2, 0, 3, 5};
        }

        return Math.round((baseTemp + monthlyOffset[month - 1] * seasonalFactor) * 10.0) / 10.0;
    }

    /**
     * Check if weather service is enabled
     */
    public boolean isEnabled() {
        return appFeaturesConfig.getFeatures().isWeather();
    }

    /**
     * Get mock weather data for testing when API is disabled
     */
    public WeatherInfo getMockWeatherInfo(BeachLocation location) {
        logger.info("Returning mock weather data for: {}", location.getName());

        WeatherInfo weatherInfo = new WeatherInfo();
        weatherInfo.setLocation(location.getName());

        // Mock data based on typical Mediterranean climate
        if (location.getName().toLowerCase().contains("benidorm")) {
            weatherInfo.setAirTemperature(24.5);
            weatherInfo.setWaterTemperature(20.2);
            weatherInfo.setDescription("Partly cloudy");
            weatherInfo.setHumidity(65);
            weatherInfo.setWindSpeed(3.2);
        } else if (location.getName().toLowerCase().contains("valencia")) {
            weatherInfo.setAirTemperature(22.8);
            weatherInfo.setWaterTemperature(19.5);
            weatherInfo.setDescription("Clear sky");
            weatherInfo.setHumidity(58);
            weatherInfo.setWindSpeed(2.1);
        } else {
            // Default Mediterranean values
            weatherInfo.setAirTemperature(23.0);
            weatherInfo.setWaterTemperature(19.8);
            weatherInfo.setDescription("Clear sky");
            weatherInfo.setHumidity(62);
            weatherInfo.setWindSpeed(2.8);
        }

        weatherInfo.setSource("Mock Data (for testing)");
        return weatherInfo;
    }
}