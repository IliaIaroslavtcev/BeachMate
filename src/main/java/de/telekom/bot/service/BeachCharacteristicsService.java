package de.telekom.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.telekom.bot.config.AppFeaturesConfig;
import de.telekom.bot.model.BeachLocation;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service for retrieving beach surface and characteristics information
 */
@Service
@RequiredArgsConstructor
public class BeachCharacteristicsService {

    private static final Logger logger = LoggerFactory.getLogger(BeachCharacteristicsService.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AppFeaturesConfig appFeaturesConfig;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    // Search radius for nearby beach features
    private static final double SEARCH_RADIUS_KM = 5.0;

    // Surface type mapping from various sources
    private static final Map<String, String> SURFACE_MAPPING = Map.of(
            "sand", "Sandy",
            "pebbles", "Pebble",
            "shingle", "Shingle",
            "rocks", "Rocky",
            "stone", "Rocky",
            "gravel", "Gravel",
            "mixed", "Mixed",
            "artificial", "Artificial"
    );

    // Common Spanish beach types and their characteristics
    private static final Map<String, BeachInfo> SPANISH_BEACH_DATABASE;

    static {
        Map<String, BeachInfo> database = new HashMap<>();

        // Costa Brava
        database.put("platja d'aro", new BeachInfo("Sand", "Natural", "Protected bay"));
        database.put("tossa de mar", new BeachInfo("Sand", "Natural", "Historic cove"));
        database.put("lloret de mar", new BeachInfo("Sand", "Natural", "Tourist beach"));

        // Costa del Sol  
        database.put("marbella", new BeachInfo("Sand", "Natural", "Golden sand"));
        database.put("nerja", new BeachInfo("Mixed", "Natural", "Coves and beaches"));
        database.put("torremolinos", new BeachInfo("Sand", "Natural", "Dark sand"));

        // Costa Blanca
        database.put("benidorm", new BeachInfo("Sand", "Natural", "Fine golden sand"));
        database.put("calpe", new BeachInfo("Sand", "Natural", "Wide sandy beach"));
        database.put("denia", new BeachInfo("Sand", "Natural", "Fine sand"));

        // Valencia region
        database.put("valencia", new BeachInfo("Sand", "Natural", "Urban beach"));
        database.put("gandia", new BeachInfo("Sand", "Natural", "Fine white sand"));
        database.put("cullera", new BeachInfo("Sand", "Natural", "Family beach"));

        // Balearic Islands
        database.put("palma", new BeachInfo("Sand", "Natural", "City beaches"));
        database.put("cala millor", new BeachInfo("Sand", "Natural", "White sand"));
        database.put("magaluf", new BeachInfo("Sand", "Natural", "Tourist beach"));

        // Canary Islands
        database.put("las canteras", new BeachInfo("Sand", "Natural", "Golden sand"));
        database.put("maspalomas", new BeachInfo("Sand", "Natural", "Sand dunes"));
        database.put("playa del ingles", new BeachInfo("Sand", "Natural", "Dark sand"));

        // Northern Spain
        database.put("san sebastian", new BeachInfo("Sand", "Natural", "Shell-shaped bay"));
        database.put("santander", new BeachInfo("Sand", "Natural", "Protected bay"));
        database.put("gijon", new BeachInfo("Sand", "Natural", "Urban beach"));

        SPANISH_BEACH_DATABASE = Collections.unmodifiableMap(database);
    }

    private static class BeachInfo {
        final String surface;
        final String type;
        final String characteristics;

        BeachInfo(String surface, String type, String characteristics) {
            this.surface = surface;
            this.type = type;
            this.characteristics = characteristics;
        }
    }

    /**
     * Enhance beach location with surface and characteristics information
     */
    public void enhanceBeachLocation(BeachLocation location) {
        if (location == null || !location.isFound()) {
            logger.warn("Cannot enhance invalid beach location");
            return;
        }

        logger.info("Enhancing beach location: {} at {}, {}",
                location.getName(), location.getLatitude(), location.getLongitude());

        // Try multiple approaches in parallel
        CompletableFuture<BeachInfo> dbLookup = CompletableFuture.supplyAsync(() ->
                lookupInDatabase(location), executorService);

        CompletableFuture<BeachInfo> osmLookup = CompletableFuture.supplyAsync(() ->
                getFromOverpassAPI(location), executorService);

        try {
            // Wait for both lookups (max 5 seconds)
            CompletableFuture.allOf(dbLookup, osmLookup).get(5, TimeUnit.SECONDS);

            // Combine results with priority: OSM data > Database > Fallback
            BeachInfo osmInfo = osmLookup.get();
            BeachInfo dbInfo = dbLookup.get();

            BeachInfo finalInfo = combineBeachInfo(osmInfo, dbInfo, location);

            // Apply the information to the location
            if (finalInfo != null) {
                location.setBeachSurface(finalInfo.surface);
                location.setBeachType(finalInfo.type);
                location.setAccessType("Easy access"); // Default for now

                logger.info("Enhanced beach info: surface={}, type={}",
                        finalInfo.surface, finalInfo.type);
            }

        } catch (Exception e) {
            logger.warn("Failed to enhance beach location", e);
            // Apply fallback information
            applyFallbackInfo(location);
        }
    }

    /**
     * Look up beach info in our curated database
     */
    private BeachInfo lookupInDatabase(BeachLocation location) {
        if (location.getName() == null) return null;

        String searchKey = location.getName().toLowerCase()
                .replace("playa de ", "")
                .replace("playa ", "")
                .replace("platja de ", "")
                .replace("platja ", "")
                .trim();

        // Try exact match first
        BeachInfo info = SPANISH_BEACH_DATABASE.get(searchKey);
        if (info != null) {
            logger.debug("Found exact database match for: {}", searchKey);
            return info;
        }

        // Try partial matches
        for (Map.Entry<String, BeachInfo> entry : SPANISH_BEACH_DATABASE.entrySet()) {
            if (searchKey.contains(entry.getKey()) || entry.getKey().contains(searchKey)) {
                logger.debug("Found partial database match: {} -> {}", searchKey, entry.getKey());
                return entry.getValue();
            }
        }

        // Try display name
        if (location.getDisplayName() != null) {
            String displayKey = location.getDisplayName().toLowerCase();
            for (Map.Entry<String, BeachInfo> entry : SPANISH_BEACH_DATABASE.entrySet()) {
                if (displayKey.contains(entry.getKey())) {
                    logger.debug("Found display name match: {} -> {}", displayKey, entry.getKey());
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    /**
     * Get beach surface info from OpenStreetMap via Overpass API
     */
    private BeachInfo getFromOverpassAPI(BeachLocation location) {
        try {
            // Search for beach features near the location
            String overpassQuery = String.format(
                    "[out:json][timeout:5];" +
                            "(node[\"natural\"=\"beach\"](around:%d,%f,%f);" +
                            " way[\"natural\"=\"beach\"](around:%d,%f,%f);" +
                            " relation[\"natural\"=\"beach\"](around:%d,%f,%f););" +
                            "out geom;",
                    (int) (SEARCH_RADIUS_KM * 1000), location.getLatitude(), location.getLongitude(),
                    (int) (SEARCH_RADIUS_KM * 1000), location.getLatitude(), location.getLongitude(),
                    (int) (SEARCH_RADIUS_KM * 1000), location.getLatitude(), location.getLongitude()
            );

            String url = "https://overpass-api.de/api/interpreter";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString("data=" + URLEncoder.encode(overpassQuery, StandardCharsets.UTF_8)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseOverpassResponse(response.body());
            } else {
                logger.warn("Overpass API returned status: {}", response.statusCode());
            }

        } catch (Exception e) {
            logger.debug("Failed to get data from Overpass API", e);
        }

        return null;
    }

    /**
     * Parse Overpass API response for beach characteristics
     */
    private BeachInfo parseOverpassResponse(String jsonResponse) throws IOException {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode elements = root.get("elements");

        if (elements == null || !elements.isArray() || elements.size() == 0) {
            return null;
        }

        // Look through elements for surface information
        for (JsonNode element : elements) {
            JsonNode tags = element.get("tags");
            if (tags != null) {
                // Check for surface tag
                String surface = tags.path("surface").asText("");
                if (!surface.isEmpty() && SURFACE_MAPPING.containsKey(surface.toLowerCase())) {
                    String mappedSurface = SURFACE_MAPPING.get(surface.toLowerCase());

                    // Get additional characteristics
                    String description = tags.path("description").asText("");
                    String access = tags.path("access").asText("Easy access");

                    logger.debug("Found OSM beach data: surface={}, description={}", mappedSurface, description);
                    return new BeachInfo(mappedSurface, "Natural", description.isEmpty() ? "Beach" : description);
                }

                // Check name for clues
                String name = tags.path("name").asText("").toLowerCase();
                if (name.contains("sand")) {
                    return new BeachInfo("Sandy", "Natural", "Sandy beach");
                } else if (name.contains("rock") || name.contains("stone")) {
                    return new BeachInfo("Rocky", "Natural", "Rocky coastline");
                } else if (name.contains("pebble")) {
                    return new BeachInfo("Pebble", "Natural", "Pebble beach");
                }
            }
        }

        return null;
    }

    /**
     * Combine information from multiple sources
     */
    private BeachInfo combineBeachInfo(BeachInfo osmInfo, BeachInfo dbInfo, BeachLocation location) {
        // OSM data has priority if available
        if (osmInfo != null && osmInfo.surface != null) {
            return osmInfo;
        }

        // Fall back to database info
        if (dbInfo != null) {
            return dbInfo;
        }

        // Generate smart fallback based on location name and region
        return generateSmartFallback(location);
    }

    /**
     * Generate smart fallback based on location characteristics
     */
    private BeachInfo generateSmartFallback(BeachLocation location) {
        if (location.getName() == null) {
            return new BeachInfo("Sand", "Natural", "Beach");
        }

        String name = location.getName().toLowerCase();
        String displayName = location.getDisplayName() != null ? location.getDisplayName().toLowerCase() : "";

        // Analyze name for clues
        if (name.contains("cala")) {
            return new BeachInfo("Mixed", "Natural", "Small cove");
        }

        if (name.contains("arena") || name.contains("sand")) {
            return new BeachInfo("Sandy", "Natural", "Sandy beach");
        }

        if (name.contains("rock") || name.contains("stone") || name.contains("piedra")) {
            return new BeachInfo("Rocky", "Natural", "Rocky coastline");
        }

        // Regional defaults for Spain
        if (displayName.contains("costa del sol") || displayName.contains("malaga")) {
            return new BeachInfo("Sand", "Natural", "Dark sand typical of Costa del Sol");
        }

        if (displayName.contains("costa brava") || displayName.contains("girona")) {
            return new BeachInfo("Mixed", "Natural", "Coves and sandy beaches");
        }

        if (displayName.contains("costa blanca") || displayName.contains("alicante") || displayName.contains("valencia")) {
            return new BeachInfo("Sand", "Natural", "Fine golden sand");
        }

        if (displayName.contains("balear") || displayName.contains("mallorca") || displayName.contains("ibiza")) {
            return new BeachInfo("Sand", "Natural", "White Mediterranean sand");
        }

        if (displayName.contains("canarias") || displayName.contains("canary")) {
            return new BeachInfo("Sand", "Natural", "Volcanic sand beaches");
        }

        if (displayName.contains("asturias") || displayName.contains("cantabria") || displayName.contains("galicia")) {
            return new BeachInfo("Sand", "Natural", "Atlantic coast beach");
        }

        // Default for Spanish beaches
        return new BeachInfo("Sand", "Natural", "Spanish beach");
    }

    /**
     * Apply fallback information when all else fails
     */
    private void applyFallbackInfo(BeachLocation location) {
        BeachInfo fallback = generateSmartFallback(location);
        location.setBeachSurface(fallback.surface);
        location.setBeachType(fallback.type);
        location.setAccessType("Easy access");

        logger.info("Applied fallback beach info: surface={}", fallback.surface);
    }

    /**
     * Check if beach characteristics service is enabled
     */
    public boolean isEnabled() {
        return appFeaturesConfig.getFeatures().isWeather(); // Reuse weather feature flag
    }
}