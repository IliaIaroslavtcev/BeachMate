package de.telekom.bot.service;

import de.telekom.bot.config.ApiConfigurationProperties;
import de.telekom.bot.model.BeachLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Debug test to see exactly what's happening with our Nominatim requests
 */
public class NominatimDebugTest {
    
    @Test
    public void debugNominatimRequest() throws IOException, InterruptedException {
        System.out.println("=== Debug Nominatim Request ===");
        
        // Test the exact same request format that works in browser
        String workingUrl = "https://nominatim.openstreetmap.org/search?q=Benidorm&format=json";
        
        System.out.println("Testing working URL: " + workingUrl);
        
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
            
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(workingUrl))
            .header("User-Agent", "Spanish Beach Bot/1.0 (test)")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
            
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Response Status: " + response.statusCode());
        System.out.println("Response Headers: " + response.headers().map());
        System.out.println("Response Body: " + response.body());
        
        System.out.println("\n=== Now testing our service ===");
        
        ApiConfigurationProperties config = new ApiConfigurationProperties();
        GeocodeService geocodeService = new GeocodeService(config, new de.telekom.bot.config.AppFeaturesConfig());
        
        BeachLocation location = geocodeService.findBeachCoordinates("Benidorm");
        
        System.out.println("Service result - Found: " + location.isFound());
        if (location.isFound()) {
            System.out.println("Display Name: " + location.getDisplayName());
            System.out.println("Coordinates: " + location.getLatitude() + ", " + location.getLongitude());
        }
    }
}