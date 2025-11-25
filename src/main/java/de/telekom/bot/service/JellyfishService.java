package de.telekom.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.telekom.bot.config.AppFeaturesConfig;
import de.telekom.bot.model.BeachLocation;
import de.telekom.bot.model.JellyfishInfo;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for retrieving jellyfish information from multiple open APIs
 */
@Service
@RequiredArgsConstructor
public class JellyfishService {

    private static final Logger logger = LoggerFactory.getLogger(JellyfishService.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)) // Reduced timeout
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AppFeaturesConfig appFeaturesConfig;
    private final ExecutorService executorService = Executors.newFixedThreadPool(3); // For parallel API calls

    // Search radius in kilometers
    private static final double SEARCH_RADIUS_KM = 50.0;

    // API timeouts (reduced for faster response)
    private static final Duration API_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration FAST_TIMEOUT = Duration.ofSeconds(5);

    // Simple cache to avoid repeated API calls (5 minute cache)
    private final Map<String, JellyfishInfo> cache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> cacheTimestamps = new ConcurrentHashMap<>();
    private static final Duration CACHE_DURATION = Duration.ofMinutes(5);

    // Known dangerous jellyfish species
    private static final Map<String, JellyfishInfo.JellyfishSighting.SeverityLevel> SPECIES_SEVERITY = Map.of(
            "Physalia physalis", JellyfishInfo.JellyfishSighting.SeverityLevel.EXTREME, // Portuguese Man o' War
            "Chironex fleckeri", JellyfishInfo.JellyfishSighting.SeverityLevel.EXTREME,   // Box Jellyfish
            "Carybdea", JellyfishInfo.JellyfishSighting.SeverityLevel.DANGEROUS,          // Box Jellyfish family
            "Pelagia noctiluca", JellyfishInfo.JellyfishSighting.SeverityLevel.PAINFUL,   // Mauve Stinger
            "Chrysaora", JellyfishInfo.JellyfishSighting.SeverityLevel.PAINFUL,           // Sea Nettle
            "Aurelia aurita", JellyfishInfo.JellyfishSighting.SeverityLevel.MILD,         // Moon Jellyfish
            "Rhizostoma pulmo", JellyfishInfo.JellyfishSighting.SeverityLevel.MILD        // Barrel Jellyfish
    );

    // Common names mapping
    private static final Map<String, String> COMMON_NAMES = Map.of(
            "Physalia physalis", "Portuguese Man o' War",
            "Chironex fleckeri", "Box Jellyfish",
            "Pelagia noctiluca", "Mauve Stinger",
            "Chrysaora quinquecirrha", "Sea Nettle",
            "Chrysaora", "Sea Nettle",
            "Aurelia aurita", "Moon Jellyfish",
            "Aurelia", "Moon Jellyfish",
            "Rhizostoma pulmo", "Barrel Jellyfish",
            "Rhizostoma", "Barrel Jellyfish",
            "Cnidaria", "Jellyfish"
    );

    /**
     * Get jellyfish information for a beach location
     */
    public JellyfishInfo getJellyfishInfo(BeachLocation location) {
        if (location == null || !location.isFound()) {
            logger.warn("Cannot get jellyfish info for invalid location");
            return createEmptyInfo();
        }

        // Check cache first
        String cacheKey = String.format("%.4f,%.4f", location.getLatitude(), location.getLongitude());
        JellyfishInfo cachedInfo = getCachedInfo(cacheKey);
        if (cachedInfo != null) {
            logger.info("Using cached jellyfish info for location: {}", location.getName());
            return cachedInfo;
        }

        logger.info("Getting fresh jellyfish info for location: {} at {}, {}",
                location.getName(), location.getLatitude(), location.getLongitude());

        JellyfishInfo info = new JellyfishInfo();
        info.setLocation(location.getName());
        info.setLatitude(location.getLatitude());
        info.setLongitude(location.getLongitude());
        info.setLastUpdated(LocalDateTime.now());
        info.setSource("GBIF + iNaturalist + OBIS");

        // Set current location for async methods
        this.currentLocation = location;

        List<JellyfishInfo.JellyfishSighting> allSightings = new ArrayList<>();

        // Get data from multiple sources in parallel for speed
        CompletableFuture<List<JellyfishInfo.JellyfishSighting>> inatFuture =
                CompletableFuture.supplyAsync(this::safeGetINaturalistSightings, executorService)
                        .orTimeout(FAST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                        .exceptionally(t -> {
                            logger.warn("iNaturalist API failed: {}", t.getMessage());
                            return Collections.emptyList();
                        });

        CompletableFuture<List<JellyfishInfo.JellyfishSighting>> gbifFuture =
                CompletableFuture.supplyAsync(this::safeGetGBIFSightings, executorService)
                        .orTimeout(API_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                        .exceptionally(t -> {
                            logger.warn("GBIF API failed: {}", t.getMessage());
                            return Collections.emptyList();
                        });

        CompletableFuture<List<JellyfishInfo.JellyfishSighting>> obisFuture =
                CompletableFuture.supplyAsync(this::safeGetOBISSightings, executorService)
                        .orTimeout(API_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                        .exceptionally(t -> {
                            logger.warn("OBIS API failed: {}", t.getMessage());
                            return Collections.emptyList();
                        });

        // Wait for all API calls to complete (with overall timeout)
        try {
            CompletableFuture.allOf(inatFuture, gbifFuture, obisFuture)
                    .get(10, TimeUnit.SECONDS); // Overall timeout of 10 seconds

            // Collect results
            allSightings.addAll(inatFuture.get());
            allSightings.addAll(gbifFuture.get());
            allSightings.addAll(obisFuture.get());

        } catch (TimeoutException e) {
            logger.warn("API calls timed out, using partial data");
            // Get whatever data is available
            if (inatFuture.isDone() && !inatFuture.isCompletedExceptionally()) {
                try {
                    allSightings.addAll(inatFuture.get());
                } catch (Exception ex) {
                }
            }
            if (gbifFuture.isDone() && !gbifFuture.isCompletedExceptionally()) {
                try {
                    allSightings.addAll(gbifFuture.get());
                } catch (Exception ex) {
                }
            }
            if (obisFuture.isDone() && !obisFuture.isCompletedExceptionally()) {
                try {
                    allSightings.addAll(obisFuture.get());
                } catch (Exception ex) {
                }
            }
        } catch (Exception e) {
            logger.error("Error waiting for API calls", e);
        }

        // Process and analyze sightings
        allSightings = processAndDeduplicateSightings(allSightings, location);
        info.setRecentSightings(allSightings);

        // Calculate risk level
        info.setRiskLevel(calculateRiskLevel(allSightings));
        info.setHasPrediction(true);

        // Generate prediction and advice
        info.setPrediction(generatePrediction(allSightings, info.getRiskLevel()));
        info.setSafetyAdvice(generateSafetyAdvice(allSightings, info.getRiskLevel()));

        logger.info("Jellyfish analysis complete: {} recent sightings, risk level: {}",
                allSightings.size(), info.getRiskLevel());

        // Cache the result
        cacheInfo(cacheKey, info);

        return info;
    }

    /**
     * Safe wrapper for iNaturalist API call (for async use)
     */
    private List<JellyfishInfo.JellyfishSighting> safeGetINaturalistSightings() {
        try {
            return getINaturalistSightings(currentLocation.getLatitude(), currentLocation.getLongitude());
        } catch (Exception e) {
            logger.error("Failed to get iNaturalist data", e);
            return Collections.emptyList();
        }
    }

    /**
     * Safe wrapper for GBIF API call (for async use)
     */
    private List<JellyfishInfo.JellyfishSighting> safeGetGBIFSightings() {
        try {
            return getGBIFSightings(currentLocation.getLatitude(), currentLocation.getLongitude());
        } catch (Exception e) {
            logger.error("Failed to get GBIF data", e);
            return Collections.emptyList();
        }
    }

    /**
     * Safe wrapper for OBIS API call (for async use)
     */
    private List<JellyfishInfo.JellyfishSighting> safeGetOBISSightings() {
        try {
            return getOBISSightings(currentLocation.getLatitude(), currentLocation.getLongitude());
        } catch (Exception e) {
            logger.error("Failed to get OBIS data", e);
            return Collections.emptyList();
        }
    }

    // Временная переменная для передачи location в async методы
    private BeachLocation currentLocation;

    /**
     * Get jellyfish sightings from iNaturalist API
     */
    private List<JellyfishInfo.JellyfishSighting> getINaturalistSightings(double lat, double lon)
            throws IOException, InterruptedException {

        // Search for Cnidaria (jellyfish and related) within radius
        String url = String.format(
                "https://api.inaturalist.org/v1/observations?taxon_name=Cnidaria&lat=%f&lng=%f&radius=%d&per_page=20&order=desc&order_by=observed_on",
                lat, lon, (int) SEARCH_RADIUS_KM
        );

        logger.debug("iNaturalist request: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Spanish Beach Bot/1.0")
                .timeout(FAST_TIMEOUT) // Reduced timeout for faster response
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.warn("iNaturalist API returned status: {}", response.statusCode());
            return Collections.emptyList();
        }

        return parseINaturalistResponse(response.body(), lat, lon);
    }

    /**
     * Parse iNaturalist API response
     */
    private List<JellyfishInfo.JellyfishSighting> parseINaturalistResponse(String jsonResponse, double centerLat, double centerLon)
            throws IOException {

        List<JellyfishInfo.JellyfishSighting> sightings = new ArrayList<>();
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode results = root.get("results");

        if (results == null || !results.isArray()) {
            return sightings;
        }

        for (JsonNode obs : results) {
            try {
                JellyfishInfo.JellyfishSighting sighting = new JellyfishInfo.JellyfishSighting();

                // Extract species information
                JsonNode taxon = obs.get("taxon");
                if (taxon != null) {
                    String scientificName = taxon.get("name").asText();
                    sighting.setSpecies(scientificName);

                    // Try to get a meaningful common name
                    String commonName = getCommonName(scientificName, taxon);
                    sighting.setCommonName(commonName);
                }

                // Extract location
                String location = obs.path("location").asText();
                if (!location.isEmpty()) {
                    String[] coords = location.split(",");
                    if (coords.length == 2) {
                        sighting.setLatitude(Double.parseDouble(coords[0].trim()));
                        sighting.setLongitude(Double.parseDouble(coords[1].trim()));
                        sighting.setDistanceKm(calculateDistance(centerLat, centerLon,
                                sighting.getLatitude(), sighting.getLongitude()));
                    }
                }

                // Extract date
                String observedOn = obs.path("observed_on").asText();
                if (!observedOn.isEmpty()) {
                    LocalDateTime observedDate = LocalDateTime.parse(observedOn + "T12:00:00");
                    sighting.setObservedDate(observedDate);
                    sighting.setDaysAgo((int) ChronoUnit.DAYS.between(observedDate.toLocalDate(), LocalDateTime.now().toLocalDate()));
                }

                // Set severity based on species
                sighting.setSeverity(determineSeverity(sighting.getSpecies()));
                sighting.setReportedBy("iNaturalist Community");
                sighting.setVerified(true);

                sightings.add(sighting);

            } catch (Exception e) {
                logger.debug("Error parsing iNaturalist observation", e);
            }
        }

        return sightings;
    }

    /**
     * Get jellyfish sightings from GBIF API
     */
    private List<JellyfishInfo.JellyfishSighting> getGBIFSightings(double lat, double lon)
            throws IOException, InterruptedException {

        // Create a geometry for the search area (bounding box)
        double offset = 0.5; // approximately 55km
        String geometry = String.format("POLYGON((%f %f,%f %f,%f %f,%f %f,%f %f))",
                lon - offset, lat - offset,  // bottom-left
                lon - offset, lat + offset,  // top-left
                lon + offset, lat + offset,  // top-right
                lon + offset, lat - offset,  // bottom-right
                lon - offset, lat - offset   // close polygon
        );

        String url = String.format(
                "https://api.gbif.org/v1/occurrence/search?q=Cnidaria&hasCoordinate=true&geometry=%s&limit=20&country=ES",
                URLEncoder.encode(geometry, StandardCharsets.UTF_8)
        );

        logger.debug("GBIF request: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Spanish Beach Bot/1.0")
                .timeout(API_TIMEOUT) // Reduced timeout for faster response
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.warn("GBIF API returned status: {}", response.statusCode());
            return Collections.emptyList();
        }

        return parseGBIFResponse(response.body(), lat, lon);
    }

    /**
     * Parse GBIF API response
     */
    private List<JellyfishInfo.JellyfishSighting> parseGBIFResponse(String jsonResponse, double centerLat, double centerLon)
            throws IOException {

        List<JellyfishInfo.JellyfishSighting> sightings = new ArrayList<>();
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode results = root.get("results");

        if (results == null || !results.isArray()) {
            return sightings;
        }

        for (JsonNode record : results) {
            try {
                JellyfishInfo.JellyfishSighting sighting = new JellyfishInfo.JellyfishSighting();

                // Extract species
                String scientificName = record.path("scientificName").asText("");
                sighting.setSpecies(scientificName);

                // Try to get a meaningful common name
                String commonName = getCommonName(scientificName, record);
                sighting.setCommonName(commonName);

                // Extract coordinates
                double recLat = record.path("decimalLatitude").asDouble(0);
                double recLon = record.path("decimalLongitude").asDouble(0);
                if (recLat != 0 && recLon != 0) {
                    sighting.setLatitude(recLat);
                    sighting.setLongitude(recLon);
                    sighting.setDistanceKm(calculateDistance(centerLat, centerLon, recLat, recLon));
                }

                // Extract date
                String eventDate = record.path("eventDate").asText();
                if (!eventDate.isEmpty()) {
                    try {
                        LocalDateTime date = LocalDateTime.parse(eventDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        sighting.setObservedDate(date);
                        sighting.setDaysAgo((int) ChronoUnit.DAYS.between(date.toLocalDate(), LocalDateTime.now().toLocalDate()));
                    } catch (Exception e) {
                        // Try different date format
                        try {
                            LocalDateTime date = LocalDateTime.parse(eventDate + "T12:00:00");
                            sighting.setObservedDate(date);
                            sighting.setDaysAgo((int) ChronoUnit.DAYS.between(date.toLocalDate(), LocalDateTime.now().toLocalDate()));
                        } catch (Exception ex) {
                            logger.debug("Could not parse date: {}", eventDate);
                        }
                    }
                }

                sighting.setSeverity(determineSeverity(sighting.getSpecies()));
                sighting.setReportedBy("GBIF Network");
                sighting.setVerified(true);

                sightings.add(sighting);

            } catch (Exception e) {
                logger.debug("Error parsing GBIF record", e);
            }
        }

        return sightings;
    }

    /**
     * Get jellyfish sightings from OBIS API
     */
    private List<JellyfishInfo.JellyfishSighting> getOBISSightings(double lat, double lon)
            throws IOException, InterruptedException {

        // Create polygon for search area
        double offset = 0.5;
        String geometry = String.format("POLYGON((%f %f,%f %f,%f %f,%f %f,%f %f))",
                lon - offset, lat + offset,  // top-left
                lon + offset, lat + offset,  // top-right
                lon + offset, lat - offset,  // bottom-right
                lon - offset, lat - offset,  // bottom-left
                lon - offset, lat + offset   // close
        );

        String url = String.format(
                "https://api.obis.org/v3/occurrence?geometry=%s&scientificname=Cnidaria&size=20",
                URLEncoder.encode(geometry, StandardCharsets.UTF_8)
        );

        logger.debug("OBIS request: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Spanish Beach Bot/1.0")
                .timeout(API_TIMEOUT) // Reduced timeout for faster response
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.warn("OBIS API returned status: {}", response.statusCode());
            return Collections.emptyList();
        }

        return parseOBISResponse(response.body(), lat, lon);
    }

    /**
     * Parse OBIS API response
     */
    private List<JellyfishInfo.JellyfishSighting> parseOBISResponse(String jsonResponse, double centerLat, double centerLon)
            throws IOException {

        List<JellyfishInfo.JellyfishSighting> sightings = new ArrayList<>();
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode results = root.get("results");

        if (results == null || !results.isArray()) {
            return sightings;
        }

        for (JsonNode record : results) {
            try {
                JellyfishInfo.JellyfishSighting sighting = new JellyfishInfo.JellyfishSighting();

                // Extract species
                String species = record.path("species").asText();
                if (species.isEmpty()) {
                    species = record.path("scientificName").asText("");
                }
                sighting.setSpecies(species);

                // Try to get a meaningful common name
                String commonName = getCommonName(species, record);
                sighting.setCommonName(commonName);

                // Extract coordinates
                double recLat = record.path("decimalLatitude").asDouble(0);
                double recLon = record.path("decimalLongitude").asDouble(0);
                if (recLat != 0 && recLon != 0) {
                    sighting.setLatitude(recLat);
                    sighting.setLongitude(recLon);
                    sighting.setDistanceKm(calculateDistance(centerLat, centerLon, recLat, recLon));
                }

                // Extract date (OBIS uses timestamps)
                long dateMid = record.path("date_mid").asLong(0);
                if (dateMid > 0) {
                    LocalDateTime date = LocalDateTime.ofEpochSecond(dateMid / 1000, 0, java.time.ZoneOffset.UTC);
                    sighting.setObservedDate(date);
                    sighting.setDaysAgo((int) ChronoUnit.DAYS.between(date.toLocalDate(), LocalDateTime.now().toLocalDate()));
                }

                sighting.setSeverity(determineSeverity(sighting.getSpecies()));
                sighting.setReportedBy("OBIS Network");
                sighting.setVerified(true);

                sightings.add(sighting);

            } catch (Exception e) {
                logger.debug("Error parsing OBIS record", e);
            }
        }

        return sightings;
    }

    /**
     * Get meaningful common name for a species
     */
    private String getCommonName(String scientificName, JsonNode data) {
        if (scientificName == null || scientificName.trim().isEmpty()) {
            return null; // Will be filtered out
        }

        // First check our known species mapping
        for (Map.Entry<String, String> entry : COMMON_NAMES.entrySet()) {
            if (scientificName.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        // Try to extract from API response
        if (data != null) {
            // Try different common name fields
            String apiCommonName = data.path("preferred_common_name").asText(
                    data.path("vernacularName").asText(
                            data.path("common_name").asText("")
                    )
            );

            if (!apiCommonName.isEmpty() && !apiCommonName.equals("null")) {
                return apiCommonName;
            }
        }

        // Try to make scientific name more readable
        if (scientificName.contains(" ")) {
            // Take genus name and make it readable
            String genus = scientificName.split(" ")[0];
            return makeReadable(genus) + " Jellyfish";
        }

        // If it's just "Cnidaria", return generic name
        if (scientificName.equalsIgnoreCase("cnidaria")) {
            return "Jellyfish";
        }

        // Last resort - make the scientific name readable
        return makeReadable(scientificName);
    }

    /**
     * Make scientific name more readable
     */
    private String makeReadable(String scientificName) {
        if (scientificName == null || scientificName.trim().isEmpty()) {
            return null;
        }

        String readable = scientificName.trim();

        // Capitalize first letter, lowercase the rest
        if (readable.length() > 1) {
            readable = readable.substring(0, 1).toUpperCase() + readable.substring(1).toLowerCase();
        } else {
            readable = readable.toUpperCase();
        }

        return readable;
    }

    /**
     * Process and deduplicate sightings, keep most relevant ones
     */
    private List<JellyfishInfo.JellyfishSighting> processAndDeduplicateSightings(
            List<JellyfishInfo.JellyfishSighting> sightings, BeachLocation location) {

        return sightings.stream()
                // Filter out old sightings (older than 30 days)
                .filter(s -> s.getDaysAgo() <= 30 && s.getDaysAgo() >= 0)
                // Filter by distance (within search radius)
                .filter(s -> s.getDistanceKm() <= SEARCH_RADIUS_KM)
                // Filter out sightings with missing or poor quality data
                .filter(s -> s.getCommonName() != null && !s.getCommonName().trim().isEmpty())
                .filter(s -> s.getSpecies() != null && !s.getSpecies().trim().isEmpty())
                // Sort by relevance (recent, close, dangerous first)
                .sorted((a, b) -> {
                    // Dangerous species first
                    int severityCompare = b.getSeverity().compareTo(a.getSeverity());
                    if (severityCompare != 0) return severityCompare;

                    // Then by recency
                    int daysCompare = Integer.compare(a.getDaysAgo(), b.getDaysAgo());
                    if (daysCompare != 0) return daysCompare;

                    // Then by distance
                    return Double.compare(a.getDistanceKm(), b.getDistanceKm());
                })
                // Take top 10 most relevant
                .limit(10)
                .toList();
    }

    /**
     * Calculate distance between two coordinates in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // Earth's radius in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Determine severity level based on species
     */
    private JellyfishInfo.JellyfishSighting.SeverityLevel determineSeverity(String species) {
        if (species == null || species.isEmpty()) {
            return JellyfishInfo.JellyfishSighting.SeverityLevel.MILD;
        }

        // Check for exact matches first
        for (Map.Entry<String, JellyfishInfo.JellyfishSighting.SeverityLevel> entry : SPECIES_SEVERITY.entrySet()) {
            if (species.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        // Default to mild for unknown species
        return JellyfishInfo.JellyfishSighting.SeverityLevel.MILD;
    }

    /**
     * Calculate overall risk level based on sightings
     */
    private JellyfishInfo.RiskLevel calculateRiskLevel(List<JellyfishInfo.JellyfishSighting> sightings) {
        if (sightings == null || sightings.isEmpty()) {
            return JellyfishInfo.RiskLevel.VERY_LOW;
        }

        // Count recent dangerous sightings
        long recentDangerous = sightings.stream()
                .filter(s -> s.getDaysAgo() <= 7)
                .filter(s -> s.getSeverity() == JellyfishInfo.JellyfishSighting.SeverityLevel.DANGEROUS ||
                        s.getSeverity() == JellyfishInfo.JellyfishSighting.SeverityLevel.EXTREME)
                .count();

        // Count very recent sightings (last 3 days)
        long veryRecentSightings = sightings.stream()
                .filter(s -> s.getDaysAgo() <= 3)
                .count();

        // Count close sightings (within 10km)
        long closeSightings = sightings.stream()
                .filter(s -> s.getDistanceKm() <= 10.0)
                .filter(s -> s.getDaysAgo() <= 14)
                .count();

        // Determine risk level
        if (recentDangerous > 0 && veryRecentSightings > 2) {
            return JellyfishInfo.RiskLevel.VERY_HIGH;
        } else if (recentDangerous > 0 || (veryRecentSightings > 3 && closeSightings > 1)) {
            return JellyfishInfo.RiskLevel.HIGH;
        } else if (closeSightings > 2 || veryRecentSightings > 1) {
            return JellyfishInfo.RiskLevel.MODERATE;
        } else if (!sightings.isEmpty()) {
            return JellyfishInfo.RiskLevel.LOW;
        } else {
            return JellyfishInfo.RiskLevel.VERY_LOW;
        }
    }

    /**
     * Generate prediction text based on sightings and risk level
     */
    private String generatePrediction(List<JellyfishInfo.JellyfishSighting> sightings, JellyfishInfo.RiskLevel riskLevel) {
        if (sightings.isEmpty()) {
            return "No recent jellyfish activity detected in this area";
        }

        int recentCount = (int) sightings.stream().filter(s -> s.getDaysAgo() <= 7).count();
        String speciesText = sightings.isEmpty() ? "" :
                sightings.stream()
                        .map(JellyfishInfo.JellyfishSighting::getCommonName)
                        .filter(name -> name != null && !name.trim().isEmpty())
                        .distinct()
                        .limit(3)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("marine life");

        return switch (riskLevel) {
            case VERY_HIGH ->
                    String.format("High risk: %d recent dangerous jellyfish sightings (%s)", recentCount, speciesText);
            case HIGH ->
                    String.format("Elevated risk: %d recent jellyfish sightings including %s", recentCount, speciesText);
            case MODERATE ->
                    String.format("Moderate activity: %d jellyfish sightings reported recently (%s)", recentCount, speciesText);
            case LOW -> String.format("Low activity: Few jellyfish sightings (%s)", speciesText);
            case VERY_LOW -> "Minimal jellyfish activity - conditions appear safe";
        };
    }

    /**
     * Generate safety advice based on sightings and risk level
     */
    private String generateSafetyAdvice(List<JellyfishInfo.JellyfishSighting> sightings, JellyfishInfo.RiskLevel riskLevel) {
        StringBuilder advice = new StringBuilder();

        // Add risk-specific advice
        advice.append(switch (riskLevel) {
            case VERY_HIGH -> "⛔ Swimming not recommended! Stay out of the water.";
            case HIGH -> "🚫 Exercise extreme caution. Consider avoiding swimming.";
            case MODERATE -> "⚠️ Check water carefully before entering. Swim with caution.";
            case LOW -> "👀 Generally safe, but remain alert for jellyfish.";
            case VERY_LOW -> "🏊‍♂️ Good swimming conditions - minimal jellyfish risk.";
        });

        // Add species-specific advice if dangerous species present
        boolean hasDangerous = sightings.stream()
                .anyMatch(s -> s.getSeverity() == JellyfishInfo.JellyfishSighting.SeverityLevel.DANGEROUS ||
                        s.getSeverity() == JellyfishInfo.JellyfishSighting.SeverityLevel.EXTREME);

        if (hasDangerous) {
            advice.append(" Dangerous species reported - seek immediate medical attention if stung.");
        }

        return advice.toString();
    }

    /**
     * Check if jellyfish monitoring is enabled
     */
    public boolean isEnabled() {
        return appFeaturesConfig.getFeatures().isWeather(); // Reuse weather feature flag for now
    }

    /**
     * Get cached jellyfish info if still valid
     */
    private JellyfishInfo getCachedInfo(String cacheKey) {
        LocalDateTime cacheTime = cacheTimestamps.get(cacheKey);
        if (cacheTime != null && Duration.between(cacheTime, LocalDateTime.now()).compareTo(CACHE_DURATION) < 0) {
            return cache.get(cacheKey);
        }

        // Clean up expired cache entry
        cache.remove(cacheKey);
        cacheTimestamps.remove(cacheKey);
        return null;
    }

    /**
     * Cache jellyfish info
     */
    private void cacheInfo(String cacheKey, JellyfishInfo info) {
        cache.put(cacheKey, info);
        cacheTimestamps.put(cacheKey, LocalDateTime.now());

        // Simple cleanup: remove oldest entries if cache gets too large
        if (cache.size() > 100) {
            String oldestKey = cacheTimestamps.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldestKey != null) {
                cache.remove(oldestKey);
                cacheTimestamps.remove(oldestKey);
            }
        }
    }

    /**
     * Create empty jellyfish info for fallback
     */
    private JellyfishInfo createEmptyInfo() {
        JellyfishInfo info = new JellyfishInfo();
        info.setRiskLevel(JellyfishInfo.RiskLevel.VERY_LOW);
        info.setRecentSightings(Collections.emptyList());
        info.setPrediction("No data available for this location");
        info.setSafetyAdvice("Check local beach conditions before swimming");
        info.setSource("No data");
        info.setLastUpdated(LocalDateTime.now());
        return info;
    }
}