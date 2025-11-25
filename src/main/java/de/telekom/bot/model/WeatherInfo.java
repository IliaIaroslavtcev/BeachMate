package de.telekom.bot.model;

import lombok.Getter;

/**
 * Model for weather information including air and water temperature
 */
@Getter
public class WeatherInfo {

    private Double airTemperature;      // Temperature in Celsius
    private Double waterTemperature;    // Water temperature in Celsius
    private String description;         // Weather description (e.g., "clear sky", "light rain")
    private Integer humidity;           // Humidity percentage
    private Double windSpeed;           // Wind speed in m/s
    private String location;            // Location name
    private boolean airTempFound;       // Whether air temperature was successfully retrieved
    private boolean waterTempFound;     // Whether water temperature was successfully retrieved
    private String source;              // Data source information
    private JellyfishInfo jellyfishInfo; // Jellyfish safety information

    // Default constructor
    public WeatherInfo() {
        this.airTempFound = false;
        this.waterTempFound = false;
    }

    // Constructor with basic temperature data
    public WeatherInfo(Double airTemperature, Double waterTemperature) {
        this.airTemperature = airTemperature;
        this.waterTemperature = waterTemperature;
        this.airTempFound = airTemperature != null;
        this.waterTempFound = waterTemperature != null;
    }

    // Custom setters with logic
    public void setAirTemperature(Double airTemperature) {
        this.airTemperature = airTemperature;
        this.airTempFound = airTemperature != null;
    }

    public void setWaterTemperature(Double waterTemperature) {
        this.waterTemperature = waterTemperature;
        this.waterTempFound = waterTemperature != null;
    }

    // Standard setters for other fields
    public void setDescription(String description) {
        this.description = description;
    }

    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
    }

    public void setWindSpeed(Double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setAirTempFound(boolean airTempFound) {
        this.airTempFound = airTempFound;
    }

    public void setWaterTempFound(boolean waterTempFound) {
        this.waterTempFound = waterTempFound;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setJellyfishInfo(JellyfishInfo jellyfishInfo) {
        this.jellyfishInfo = jellyfishInfo;
    }

    /**
     * Check if any temperature data was found
     */
    public boolean hasTemperatureData() {
        return airTempFound || waterTempFound;
    }

    /**
     * Get formatted air temperature string
     */
    public String getFormattedAirTemperature() {
        if (airTemperature == null) {
            return "N/A";
        }
        return String.format("%.1f°C", airTemperature);
    }

    /**
     * Get formatted water temperature string
     */
    public String getFormattedWaterTemperature() {
        if (waterTemperature == null) {
            return "N/A";
        }
        return String.format("%.1f°C", waterTemperature);
    }

    /**
     * Get comfort level description based on temperatures
     */
    public String getComfortDescription() {
        if (!hasTemperatureData()) {
            return "Weather data unavailable";
        }

        StringBuilder comfort = new StringBuilder();

        if (airTempFound && airTemperature != null) {
            if (airTemperature >= 25) {
                comfort.append("🌤️ Perfect beach weather");
            } else if (airTemperature >= 20) {
                comfort.append("☀️ Pleasant temperature");
            } else if (airTemperature >= 15) {
                comfort.append("🌤️ Mild weather");
            } else {
                comfort.append("🌥️ Cool weather");
            }
        }

        if (waterTempFound && waterTemperature != null) {
            if (comfort.length() > 0) {
                comfort.append(", ");
            }

            if (waterTemperature >= 22) {
                comfort.append("🌊 Warm water for swimming");
            } else if (waterTemperature >= 18) {
                comfort.append("🌊 Water suitable for swimming");
            } else if (waterTemperature >= 15) {
                comfort.append("🌊 Cool water");
            } else {
                comfort.append("🌊 Cold water");
            }
        }

        // Add jellyfish safety info
        if (jellyfishInfo != null && jellyfishInfo.getRiskLevel() != null) {
            if (comfort.length() > 0) {
                comfort.append(", ");
            }

            switch (jellyfishInfo.getRiskLevel()) {
                case VERY_HIGH -> comfort.append("⛔ Jellyfish alert: Swimming not recommended");
                case HIGH -> comfort.append("🚫 High jellyfish risk: Extreme caution advised");
                case MODERATE -> comfort.append("⚠️ Moderate jellyfish activity: Check water before entering");
                case LOW -> comfort.append("👀 Low jellyfish risk: Remain alert");
                case VERY_LOW -> comfort.append("🏊‍♂️ Minimal jellyfish activity");
            }
        }

        return comfort.toString();
    }

    @Override
    public String toString() {
        return String.format("WeatherInfo{air=%.1f°C, water=%.1f°C, description='%s', location='%s'}",
                airTemperature, waterTemperature, description, location);
    }
}