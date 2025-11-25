package de.telekom.bot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data model for Nominatim OpenStreetMap API response
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class NominatimResponse {

    @JsonProperty("place_id")
    private Long placeId;

    @JsonProperty("lat")
    private String latitude;

    @JsonProperty("lon")
    private String longitude;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("name")
    private String name;

    @JsonProperty("class")
    private String classification;

    @JsonProperty("type")
    private String type;

    @JsonProperty("importance")
    private Double importance;

}