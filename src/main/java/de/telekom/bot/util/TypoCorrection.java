package de.telekom.bot.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for handling typos and suggesting corrections
 */
public class TypoCorrection {

    // Popular Spanish beach destinations and their common misspellings
    private static final Map<String, List<String>> POPULAR_DESTINATIONS = new HashMap<>();

    static {
        // Benidorm variants
        POPULAR_DESTINATIONS.put("benidorm", Arrays.asList(
                "benidrm", "benidrom", "benidorn", "benedorm", "benidom",
                "benediorm", "binidorm", "benidrorm", "beniddorm"
        ));

        // Valencia variants
        POPULAR_DESTINATIONS.put("valencia", Arrays.asList(
                "valecia", "valensia", "valancia", "balencia"
        ));

        // Alicante variants
        POPULAR_DESTINATIONS.put("alicante", Arrays.asList(
                "alicant", "aliecante", "alikante", "alicamte"
        ));

        // Málaga variants
        POPULAR_DESTINATIONS.put("malaga", Arrays.asList(
                "malaga", "málaga", "malga", "malag", "malagas"
        ));

        // Barcelona variants
        POPULAR_DESTINATIONS.put("barcelona", Arrays.asList(
                "barcelon", "barcelona", "barselona", "barcellona", "barcalona"
        ));

        // Marbella variants  
        POPULAR_DESTINATIONS.put("marbella", Arrays.asList(
                "marbela", "marbell", "marbella", "marbela", "marbilla"
        ));

        // Torremolinos variants
        POPULAR_DESTINATIONS.put("torremolinos", Arrays.asList(
                "torremelinos", "torremolino", "toremlinos", "torremolinos"
        ));

        // Santander variants
        POPULAR_DESTINATIONS.put("santander", Arrays.asList(
                "santandr", "santande", "santander", "santadner"
        ));

        // San Sebastián / Donostia variants
        POPULAR_DESTINATIONS.put("san sebastian", Arrays.asList(
                "san sebastian", "donostia", "sansebastian", "san sebastien"
        ));

        // Cadaqués variants
        POPULAR_DESTINATIONS.put("cadaques", Arrays.asList(
                "cadaques", "cadaqués", "cadakes", "cadaquez"
        ));
    }

    /**
     * Find the best correction suggestion for a typo
     *
     * @param input User's input with potential typo
     * @return Suggested correction or null if no good match found
     */
    public static TypoCorrectionSuggestion findBestCorrection(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String normalizedInput = input.toLowerCase().trim();

        // First check exact matches (user typed correctly)
        if (POPULAR_DESTINATIONS.containsKey(normalizedInput)) {
            return null; // No correction needed
        }

        String bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;
        int maxDistance = Math.max(2, normalizedInput.length() / 3); // Allow up to 1/3 of characters to be different

        // Check against all known destinations
        for (Map.Entry<String, List<String>> entry : POPULAR_DESTINATIONS.entrySet()) {
            String destination = entry.getKey();
            List<String> variants = entry.getValue();

            // Check distance to main destination name
            int distance = levenshteinDistance(normalizedInput, destination);
            if (distance < bestDistance && distance <= maxDistance && distance > 0) {
                bestDistance = distance;
                bestMatch = destination;
            }

            // Check if input matches any known variant exactly (distance 0)
            // If so, suggest the main destination with distance calculated to main destination
            for (String variant : variants) {
                if (normalizedInput.equals(variant.toLowerCase())) {
                    // Found exact match in variants, calculate distance to main destination
                    int mainDistance = levenshteinDistance(normalizedInput, destination);
                    if (mainDistance < bestDistance && mainDistance <= maxDistance && mainDistance > 0) {
                        bestDistance = mainDistance;
                        bestMatch = destination;
                    }
                }
            }
        }

        if (bestMatch != null && bestDistance <= maxDistance) {
            return new TypoCorrectionSuggestion(bestMatch, bestDistance, normalizedInput);
        }

        return null;
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private static int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(Math.min(
                                    dp[i - 1][j] + 1,     // deletion
                                    dp[i][j - 1] + 1),    // insertion
                            dp[i - 1][j - 1] + 1  // substitution
                    );
                }
            }
        }

        return dp[len1][len2];
    }

    /**
     * Get all known destinations for debugging/testing
     */
    public static Set<String> getAllKnownDestinations() {
        return POPULAR_DESTINATIONS.keySet();
    }

    /**
     * Data class for typo correction suggestions
     */
    public static class TypoCorrectionSuggestion {
        private final String suggestedCorrection;
        private final int editDistance;
        private final String originalInput;

        public TypoCorrectionSuggestion(String suggestedCorrection, int editDistance, String originalInput) {
            this.suggestedCorrection = suggestedCorrection;
            this.editDistance = editDistance;
            this.originalInput = originalInput;
        }

        public String getSuggestedCorrection() {
            return suggestedCorrection;
        }

        public int getEditDistance() {
            return editDistance;
        }

        public String getOriginalInput() {
            return originalInput;
        }

        public String getCapitalizedSuggestion() {
            return suggestedCorrection.substring(0, 1).toUpperCase() +
                    suggestedCorrection.substring(1);
        }

        @Override
        public String toString() {
            return String.format("TypoCorrectionSuggestion{original='%s', suggested='%s', distance=%d}",
                    originalInput, suggestedCorrection, editDistance);
        }
    }
}