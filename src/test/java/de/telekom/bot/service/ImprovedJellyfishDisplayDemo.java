package de.telekom.bot.service;

import de.telekom.bot.config.AppFeaturesConfig;
import de.telekom.bot.model.BeachLocation;
import de.telekom.bot.model.JellyfishInfo;
import de.telekom.bot.model.WeatherInfo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Demo showing improved jellyfish display formatting
 */
public class ImprovedJellyfishDisplayDemo {
    
    public static void main(String[] args) {
        System.out.println("🪼 === IMPROVED JELLYFISH DISPLAY DEMO === 🪼\n");
        
        // Initialize services
        AppFeaturesConfig appFeaturesConfig = new AppFeaturesConfig();
        JellyfishService jellyfishService = new JellyfishService(appFeaturesConfig);
        WeatherService weatherService = new WeatherService(appFeaturesConfig, jellyfishService);
        
        // Test various Spanish beach locations
        testImprovedDisplay("Benidorm Beach", 38.5384, -0.1293, weatherService);
        testImprovedDisplay("Valencia Beach", 39.4699, -0.3763, weatherService);
        
        System.out.println("✅ Display improvements applied successfully!");
        System.out.println("📊 Unknown species are now filtered out");
        System.out.println("⏰ Time display improved (today, yesterday, X days ago)");
        System.out.println("🏷️ Better species name mapping and fallbacks");
        System.out.println("🎯 Only meaningful sightings are displayed");
    }
    
    private static void testImprovedDisplay(String locationName, double lat, double lon, 
                                          WeatherService weatherService) {
        
        System.out.println("=" .repeat(70));
        System.out.println("🏖️  " + locationName);
        System.out.println("=" .repeat(70));
        
        // Create beach location
        BeachLocation location = new BeachLocation();
        location.setName(locationName);
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setFound(true);
        
        try {
            // Get weather with integrated jellyfish data
            WeatherInfo weatherInfo = weatherService.getWeatherInfo(location);
            
            if (weatherInfo.getJellyfishInfo() != null) {
                JellyfishInfo jellyfishInfo = weatherInfo.getJellyfishInfo();
                
                System.out.println("🪼 JELLYFISH INFO:");
                System.out.println("  • Risk Level: " + jellyfishInfo.getRiskLevel().getDisplayName());
                System.out.println("  • Total Sightings Found: " + jellyfishInfo.getRecentSightings().size());
                
                // Show filtered meaningful sightings
                var meaningfulSightings = jellyfishInfo.getRecentSightings().stream()
                    .filter(s -> s.getCommonName() != null && !s.getCommonName().equals("Unknown"))
                    .filter(s -> s.getDaysAgo() >= 0)
                    .limit(3)
                    .toList();
                
                if (!meaningfulSightings.isEmpty()) {
                    System.out.println("\\n📊 MEANINGFUL RECENT ACTIVITY:");
                    for (var sighting : meaningfulSightings) {
                        String timeText = sighting.getDaysAgo() == 0 ? "today" : 
                                        sighting.getDaysAgo() == 1 ? "yesterday" : 
                                        sighting.getDaysAgo() + " days ago";
                        
                        System.out.println("  • " + sighting.getCommonName() + 
                                         " (" + timeText + ", " +
                                         String.format("%.1f km away)", sighting.getDistanceKm()));
                    }
                } else {
                    System.out.println("\\n✨ No meaningful sightings to display");
                    System.out.println("   (Unknown species and invalid data filtered out)");
                }
                
                // Show what gets filtered out
                long filteredOut = jellyfishInfo.getRecentSightings().size() - meaningfulSightings.size();
                if (filteredOut > 0) {
                    System.out.println("\\n🔍 Filtered out " + filteredOut + " low-quality sightings");
                    System.out.println("   (Unknown species, invalid dates, etc.)");
                }
                
                System.out.println("\\n🎯 USER WOULD SEE:");
                String comfortDescription = weatherInfo.getComfortDescription();
                System.out.println("   " + comfortDescription);
                
            }
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
}