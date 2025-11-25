package de.telekom.bot.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Model representing beach location with coordinates and metadata
 */
@Getter
@Setter
public class BeachLocation {

    private String name;
    private String displayName;
    private double latitude;
    private double longitude;
    private String type;
    private String classification;
    private Double importance;
    private boolean found;

    // Beach characteristics
    private String beachSurface;        // sand, pebbles, rocks, mixed
    private String beachType;           // natural, artificial, etc.
    private String beachCharacteristics; // calm, windy, protected, etc.
    private String accessType;          // easy, difficult, boat_only, etc.

    // Default constructor
    public BeachLocation() {
        this.found = false;
    }

    // Constructor from NominatimResponse
    public BeachLocation(NominatimResponse nominatimResponse) {
        this.name = nominatimResponse.getName();
        this.displayName = nominatimResponse.getDisplayName();
        this.latitude = Double.parseDouble(nominatimResponse.getLatitude());
        this.longitude = Double.parseDouble(nominatimResponse.getLongitude());
        this.type = nominatimResponse.getType();
        this.classification = nominatimResponse.getClassification();
        this.importance = nominatimResponse.getImportance();
        this.found = true;
    }


    public boolean isBeach() {
        // Check beach type
        if ("beach".equals(type)) {
            return true;
        }

        // Check natural features that are beaches
        if ("natural".equals(classification) && type != null && type.equals("beach")) {
            return true;
        }

        // Check if name contains beach-related words
        if (name != null) {
            String lowerName = name.toLowerCase();
            if (lowerName.contains("playa") || lowerName.contains("platja") ||
                    lowerName.contains("beach") || lowerName.contains("cala")) {
                return true;
            }
        }

        // Check if display name contains beach-related words
        if (displayName != null) {
            String lowerDisplayName = displayName.toLowerCase();
            if (lowerDisplayName.contains("playa") || lowerDisplayName.contains("platja") ||
                    lowerDisplayName.contains("beach") || lowerDisplayName.contains("cala")) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "BeachLocation{" +
                "name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", type='" + type + '\'' +
                ", found=" + found +
                '}';
    }
}