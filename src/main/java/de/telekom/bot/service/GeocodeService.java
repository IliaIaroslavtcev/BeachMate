package de.telekom.bot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.telekom.bot.config.ApiConfigurationProperties;
import de.telekom.bot.config.AppFeaturesConfig;
import de.telekom.bot.model.BeachLocation;
import de.telekom.bot.model.NominatimResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GeocodeService {

    private static final Logger logger = LoggerFactory.getLogger(GeocodeService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApiConfigurationProperties apiConfig;
    private final AppFeaturesConfig appFeaturesConfig;
    
    // Track last request time for rate limiting
    private long lastRequestTime = 0;

    // Lazy-initialized HttpClient
    private HttpClient getHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(apiConfig.getNominatim().getApi().getConnectTimeoutSeconds()))
                .build();
    }

    /**
     * Find beach coordinates using Nominatim OSM API
     *
     * @param beachName The name of the beach to search for
     * @return BeachLocation with coordinates if found, empty location if not found
     */
    public BeachLocation findBeachCoordinates(String beachName) {
        try {
            logger.info("Searching for beach coordinates: {}", beachName);

            // Check if geocoding is enabled
            if (!appFeaturesConfig.getFeatures().isGeocoding()) {
                logger.warn("Geocoding feature is disabled");
                return new BeachLocation();
            }

            // Add beach context if not already included
            String searchQuery = beachName.toLowerCase();
            boolean hasBeachKeyword = searchQuery.contains("beach") ||
                    searchQuery.contains("playa") ||
                    searchQuery.contains("platja") ||
                    searchQuery.contains("cala");

            logger.info("Original query: '{}', has beach keyword: {}", beachName, hasBeachKeyword);

            // If the query is just a city name like "benidorm", add "beach" to get beaches
            if (!hasBeachKeyword && !searchQuery.contains("beach")) {
                searchQuery = searchQuery + " beach";
                logger.info("Modified query to: '{}'", searchQuery);
            }

            // Encode the modified query
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);

            // Build request URL using configuration
            String baseUrl = apiConfig.getNominatim().getApi().getBaseUrl();
            String endpoint = apiConfig.getNominatim().getApi().getSearchEndpoint();
            int limit = apiConfig.getNominatim().getApi().getLimit();
            String countryCodes = apiConfig.getNominatim().getApi().getCountryCodes();

            // Build URL with country code restriction if configured
            String url;
            if (countryCodes != null && !countryCodes.isEmpty()) {
                url = String.format("%s%s?q=%s&format=json&limit=%d&countrycodes=%s",
                        baseUrl, endpoint, encodedQuery, limit, countryCodes);
                logger.debug("Restricting search to countries: {}", countryCodes);
            } else {
                url = String.format("%s%s?q=%s&format=json&limit=%d",
                        baseUrl, endpoint, encodedQuery, limit);
            }

            logger.debug("Making request to: {}", url);

            // Apply rate limiting
            synchronized (this) {
                long currentTime = System.currentTimeMillis();
                long timeSinceLastRequest = currentTime - lastRequestTime;
                int rateLimitMs = apiConfig.getNominatim().getApi().getRateLimitMs();
                
                if (timeSinceLastRequest < rateLimitMs) {
                    long waitTime = rateLimitMs - timeSinceLastRequest;
                    logger.debug("Rate limiting: waiting {}ms before request", waitTime);
                    Thread.sleep(waitTime);
                }
                lastRequestTime = System.currentTimeMillis();
            }
            
            // Create HTTP request using configuration
            String userAgent = apiConfig.getNominatim().getApi().getUserAgent();
            int timeoutSeconds = apiConfig.getNominatim().getApi().getTimeoutSeconds();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", userAgent)
                    .header("Referer", "https://github.com/yourusername/beach-bot")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();

            // Send request
            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseNominatimResponse(response.body(), beachName);
            } else {
                logger.warn("Nominatim API returned status code: {} for query: {}. Response: {}",
                        response.statusCode(), beachName, response.body());
                return new BeachLocation(); // Empty location
            }

        } catch (Exception e) {
            logger.error("Error searching for beach coordinates: {}", beachName, e);
            return new BeachLocation(); // Empty location
        }
    }

    /**
     * Parse Nominatim JSON response and find the best match
     */
    private BeachLocation parseNominatimResponse(String jsonResponse, String originalQuery) {
        try {
            List<NominatimResponse> responses = objectMapper.readValue(
                    jsonResponse, new TypeReference<List<NominatimResponse>>() {
                    });

            if (responses.isEmpty()) {
                logger.info("No results found for: {}", originalQuery);
                return new BeachLocation();
            }

            // Find best match - prioritize beaches and Spanish locations
            NominatimResponse bestMatch = findBestMatch(responses, originalQuery);

            if (bestMatch != null) {
                BeachLocation location = new BeachLocation(bestMatch);
                logger.info("Found coordinates for '{}': lat={}, lon={}, display_name={}, isBeach={}",
                        originalQuery, location.getLatitude(), location.getLongitude(),
                        location.getDisplayName(), location.isBeach());
                return location;
            } else {
                logger.info("No suitable match found for: {}", originalQuery);
                return new BeachLocation();
            }

        } catch (IOException e) {
            logger.error("Error parsing Nominatim response", e);
            return new BeachLocation();
        }
    }

    /**
     * Find the best matching result from Nominatim responses
     */
    private NominatimResponse findBestMatch(List<NominatimResponse> responses, String originalQuery) {
        if (responses.isEmpty()) {
            return null;
        }

        // Log all responses for debugging
        logger.debug("Found {} responses for query: {}", responses.size(), originalQuery);
        for (int i = 0; i < responses.size(); i++) {
            NominatimResponse response = responses.get(i);
            logger.debug("Response {}: name={}, type={}, class={}, display_name={}",
                    i, response.getName(), response.getType(), response.getClassification(), response.getDisplayName());
        }

        // Priority 1: Beach locations (all results are already from Spain due to countrycodes=es)
        for (NominatimResponse response : responses) {
            if (isBeachLocation(response)) {
                logger.debug("Selected beach location: {}", response.getDisplayName());
                return response;
            }
        }

        // Fallback: First result with highest importance
        NominatimResponse best = responses.get(0);
        for (NominatimResponse response : responses) {
            if (response.getImportance() != null && best.getImportance() != null &&
                    response.getImportance() > best.getImportance()) {
                best = response;
            }
        }

        logger.debug("Selected fallback result: {}", best.getDisplayName());
        return best;
    }

    /**
     * Check if the location is a beach or coastal location
     */
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

        // Check names for beach-related keywords
        if (name != null) {
            String lowerName = name.toLowerCase();
            if (lowerName.contains("playa") || lowerName.contains("beach") ||
                    lowerName.contains("costa") || lowerName.contains("cala")) {
                return true;
            }
        }

        // Check display name for beach-related keywords
        if (displayName != null) {
            String lowerDisplayName = displayName.toLowerCase();
            if (lowerDisplayName.contains("playa") || lowerDisplayName.contains("beach") ||
                    lowerDisplayName.contains("costa")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the location is in Spain
     */
    private boolean isInSpain(NominatimResponse response) {
        String displayName = response.getDisplayName();
        if (displayName == null) {
            return false;
        }

        String lowerDisplayName = displayName.toLowerCase();
        return lowerDisplayName.contains("spain") ||
                lowerDisplayName.contains("españa") ||
                lowerDisplayName.contains("catalunya") ||
                lowerDisplayName.contains("catalonia") ||
                lowerDisplayName.contains("andalusia") ||
                lowerDisplayName.contains("andalucía") ||
                lowerDisplayName.contains("valencia") ||
                lowerDisplayName.contains("comunitat valenciana") ||
                lowerDisplayName.contains("galicia") ||
                lowerDisplayName.contains("euskadi") ||
                lowerDisplayName.contains("país vasco") ||
                lowerDisplayName.contains("basque country") ||
                lowerDisplayName.contains("murcia") ||
                lowerDisplayName.contains("castilla") ||
                lowerDisplayName.contains("aragón") ||
                lowerDisplayName.contains("asturias") ||
                lowerDisplayName.contains("cantabria") ||
                lowerDisplayName.contains("la rioja") ||
                lowerDisplayName.contains("navarra") ||
                lowerDisplayName.contains("extremadura") ||
                lowerDisplayName.contains("madrid") ||
                lowerDisplayName.contains("baleares") ||
                lowerDisplayName.contains("balearic") ||
                lowerDisplayName.contains("canarias") ||
                lowerDisplayName.contains("canary");
    }
}