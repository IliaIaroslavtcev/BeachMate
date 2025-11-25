package de.telekom.bot.service;

import de.telekom.bot.config.ApiConfigurationProperties;
import de.telekom.bot.model.BeachLocation;
import de.telekom.bot.model.NominatimResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Integration test to discover beaches in Benidorm with case-insensitive search.
 * Note: Makes real HTTP requests to external APIs - may fail due to network issues.
 * Disabled by default to avoid test failures on network problems.
 */
@Tag("integration")
public class BenidormBeachesTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    @Disabled("Integration test - requires network connection to Nominatim API")
    public void testBenidormBeachSearch() {
        System.out.println("=== Searching for beaches in Benidorm ===");
        
        String[] searchQueries = {
            "Benidorm beaches",
            "benidorm beaches", 
            "BENIDORM BEACHES",
            "playa levante benidorm",
            "playa poniente benidorm",
            "benidorm playa",
            "beaches near benidorm",
            "Playa de Levante",
            "playa de poniente",
            "MAL PAS BENIDORM"
        };
        
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        
        for (String query : searchQueries) {
            System.out.println("\n--- Testing query: '" + query + "' ---");
            try {
                searchBeaches(client, query);
            } catch (IOException | InterruptedException e) {
                System.err.println("Failed to search for '" + query + "': " + e.getMessage());
            }
        }
        
        System.out.println("\n=== Testing with our service ===");
        testWithGeocodeService();
    }
    
    private void searchBeaches(HttpClient client, String query) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format("https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=10", encodedQuery);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "Spanish Beach Bot/1.0 (benidorm-test)")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
            
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            List<NominatimResponse> responses = objectMapper.readValue(
                response.body(), new TypeReference<List<NominatimResponse>>() {});
            
            System.out.println("Found " + responses.size() + " results:");
            
            for (int i = 0; i < Math.min(responses.size(), 5); i++) {
                NominatimResponse resp = responses.get(i);
                boolean isBeach = isBeachLocation(resp);
                boolean isInSpain = isInSpain(resp);
                
                System.out.printf("  %d. %s\n", i + 1, resp.getDisplayName());
                System.out.printf("     Type: %s, Class: %s, Beach: %s, Spain: %s\n", 
                    resp.getType(), resp.getClassification(), isBeach, isInSpain);
                System.out.printf("     Coordinates: %s, %s\n", resp.getLatitude(), resp.getLongitude());
            }
        } else {
            System.out.println("Error: HTTP " + response.statusCode());
        }
    }
    
    private void testWithGeocodeService() {
        System.out.println("\n--- Testing with GeocodeService ---");
        
        ApiConfigurationProperties config = new ApiConfigurationProperties();
        GeocodeService geocodeService = new GeocodeService(config, new de.telekom.bot.config.AppFeaturesConfig());
        
        String[] testQueries = {
            "benidorm",
            "BENIDORM", 
            "Benidorm",
            "playa levante benidorm",
            "playa poniente benidorm"
        };
        
        for (String query : testQueries) {
            System.out.println("\nTesting: " + query);
            BeachLocation location = geocodeService.findBeachCoordinates(query);
            
            if (location.isFound()) {
                System.out.println("✅ Found: " + location.getDisplayName());
                System.out.println("   Coordinates: " + String.format("%.6f", location.getLatitude()) + 
                                 ", " + String.format("%.6f", location.getLongitude()));
                System.out.println("   Type: " + location.getType());
                System.out.println("   Is Beach: " + location.isBeach());
            } else {
                System.out.println("❌ Not found");
            }
        }
    }
    
    private boolean isBeachLocation(NominatimResponse response) {
        String type = response.getType();
        String classification = response.getClassification();
        String name = response.getName();
        String displayName = response.getDisplayName();
        
        // Direct beach types
        if ("beach".equals(type) || "coastline".equals(type)) {
            return true;
        }
        
        // Natural features that could be beaches
        if ("natural".equals(classification)) {
            return true;
        }
        
        // Check names for beach-related keywords (case-insensitive)
        if (name != null) {
            String lowerName = name.toLowerCase();
            if (lowerName.contains("playa") || lowerName.contains("beach") || 
                lowerName.contains("costa") || lowerName.contains("cala")) {
                return true;
            }
        }
        
        // Check display name for beach-related keywords (case-insensitive)
        if (displayName != null) {
            String lowerDisplayName = displayName.toLowerCase();
            if (lowerDisplayName.contains("playa") || lowerDisplayName.contains("beach") || 
                lowerDisplayName.contains("costa")) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isInSpain(NominatimResponse response) {
        String displayName = response.getDisplayName();
        if (displayName == null) {
            return false;
        }
        
        String lowerDisplayName = displayName.toLowerCase();
        return lowerDisplayName.contains("spain") || 
               lowerDisplayName.contains("españa") ||
               lowerDisplayName.contains("valencia") ||
               lowerDisplayName.contains("alicante");
    }
}