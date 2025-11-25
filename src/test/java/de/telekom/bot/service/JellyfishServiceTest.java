package de.telekom.bot.service;

import de.telekom.bot.config.AppFeaturesConfig;
import de.telekom.bot.model.BeachLocation;
import de.telekom.bot.model.JellyfishInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for JellyfishService to verify jellyfish monitoring functionality
 */
public class JellyfishServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(JellyfishServiceTest.class);

    private JellyfishService jellyfishService;
    
    @Mock
    private AppFeaturesConfig appFeaturesConfig;
    
    @Mock 
    private AppFeaturesConfig.Features features;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(appFeaturesConfig.getFeatures()).thenReturn(features);
        when(features.isWeather()).thenReturn(true);
        
        jellyfishService = new JellyfishService(appFeaturesConfig);
    }
    
    @Test
    void testJellyfishService_ValidBeachLocation() {
        // Test with Benidorm beach coordinates
        BeachLocation benidorm = new BeachLocation();
        benidorm.setName("Playa de Levante, Benidorm");
        benidorm.setLatitude(38.5384);
        benidorm.setLongitude(-0.1293);
        benidorm.setFound(true);
        // Beach is determined by isBeach() method based on name
        
        logger.info("Testing jellyfish service with Benidorm coordinates");
        
        JellyfishInfo result = jellyfishService.getJellyfishInfo(benidorm);
        
        assertNotNull(result, "JellyfishInfo should not be null");
        assertNotNull(result.getRiskLevel(), "Risk level should be set");
        assertNotNull(result.getRecentSightings(), "Recent sightings list should be initialized");
        assertEquals("Playa de Levante, Benidorm", result.getLocation(), "Location name should match");
        assertEquals(38.5384, result.getLatitude(), 0.001, "Latitude should match");
        assertEquals(-0.1293, result.getLongitude(), 0.001, "Longitude should match");
        assertNotNull(result.getLastUpdated(), "Last updated timestamp should be set");
        
        logger.info("Jellyfish info result: Risk={}, Sightings={}, Prediction={}", 
                   result.getRiskLevel(), result.getRecentSightings().size(), result.getPrediction());
    }
    
    @Test 
    void testJellyfishService_InvalidLocation() {
        BeachLocation invalidLocation = new BeachLocation();
        invalidLocation.setName("Invalid Location");
        invalidLocation.setFound(false);
        
        logger.info("Testing jellyfish service with invalid location");
        
        JellyfishInfo result = jellyfishService.getJellyfishInfo(invalidLocation);
        
        assertNotNull(result, "JellyfishInfo should not be null even for invalid location");
        assertEquals(JellyfishInfo.RiskLevel.VERY_LOW, result.getRiskLevel(), "Risk level should be VERY_LOW for invalid location");
        assertTrue(result.getRecentSightings().isEmpty(), "Recent sightings should be empty for invalid location");
        assertEquals("No data available for this location", result.getPrediction(), "Should have no data message");
    }
    
    @Test
    void testJellyfishService_NullLocation() {
        logger.info("Testing jellyfish service with null location");
        
        JellyfishInfo result = jellyfishService.getJellyfishInfo(null);
        
        assertNotNull(result, "JellyfishInfo should not be null even for null location");
        assertEquals(JellyfishInfo.RiskLevel.VERY_LOW, result.getRiskLevel(), "Risk level should be VERY_LOW for null location");
        assertTrue(result.getRecentSightings().isEmpty(), "Recent sightings should be empty for null location");
    }
    
    @Test
    void testJellyfishService_ServiceEnabled() {
        assertTrue(jellyfishService.isEnabled(), "Service should be enabled when weather feature is enabled");
        
        // Test disabled state
        when(features.isWeather()).thenReturn(false);
        assertFalse(jellyfishService.isEnabled(), "Service should be disabled when weather feature is disabled");
    }
    
    @Test
    void testJellyfishService_VariousSpanishLocations() {
        // Test multiple Spanish coastal locations
        String[][] testLocations = {
            {"Valencia", "39.4699", "-0.3763"},
            {"Barcelona", "41.3851", "2.1734"},
            {"Málaga", "36.7213", "-4.4214"},
            {"Alicante", "38.3452", "-0.4810"},
            {"Santander", "43.4623", "-3.8099"}
        };
        
        for (String[] location : testLocations) {
            BeachLocation beachLocation = new BeachLocation();
            beachLocation.setName(location[0]);
            beachLocation.setLatitude(Double.parseDouble(location[1]));
            beachLocation.setLongitude(Double.parseDouble(location[2]));
            beachLocation.setFound(true);
            // Beach status determined by isBeach() method
            
            logger.info("Testing jellyfish service for {}", location[0]);
            
            JellyfishInfo result = jellyfishService.getJellyfishInfo(beachLocation);
            
            assertNotNull(result, "JellyfishInfo should not be null for " + location[0]);
            assertNotNull(result.getRiskLevel(), "Risk level should be set for " + location[0]);
            assertEquals(location[0], result.getLocation(), "Location name should match for " + location[0]);
            
            logger.info("{}: Risk={}, Prediction={}", 
                       location[0], result.getRiskLevel(), result.getPrediction());
        }
    }
    
    @Test
    void testJellyfishInfo_RiskLevelDisplayNames() {
        // Test that all risk levels have proper display names
        for (JellyfishInfo.RiskLevel riskLevel : JellyfishInfo.RiskLevel.values()) {
            assertNotNull(riskLevel.getDisplayName(), 
                         "Risk level " + riskLevel + " should have a display name");
            assertFalse(riskLevel.getDisplayName().trim().isEmpty(), 
                       "Risk level " + riskLevel + " display name should not be empty");
            logger.info("Risk level {}: '{}'", riskLevel, riskLevel.getDisplayName());
        }
    }
}