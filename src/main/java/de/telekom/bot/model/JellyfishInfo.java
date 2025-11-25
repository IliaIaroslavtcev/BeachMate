package de.telekom.bot.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model for jellyfish information and alerts
 */
@Data
@NoArgsConstructor
public class JellyfishInfo {

    private String location;
    private double latitude;
    private double longitude;
    private RiskLevel riskLevel;
    private List<JellyfishSighting> recentSightings = new ArrayList<>();
    private String prediction;
    private String safetyAdvice;
    private String source;
    private LocalDateTime lastUpdated;
    private boolean hasPrediction;

    public enum RiskLevel {
        VERY_LOW("Very Low", "🟢", "Safe swimming conditions"),
        LOW("Low", "🟡", "Few jellyfish expected"),
        MODERATE("Moderate", "🟠", "Some jellyfish possible"),
        HIGH("High", "🔴", "High jellyfish activity expected"),
        VERY_HIGH("Very High", "⚫", "Dangerous conditions - avoid swimming");

        private final String displayName;
        private final String emoji;
        private final String description;

        RiskLevel(String displayName, String emoji, String description) {
            this.displayName = displayName;
            this.emoji = emoji;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getEmoji() {
            return emoji;
        }

        public String getDescription() {
            return description;
        }

        public String getFormattedRisk() {
            return emoji + " " + displayName;
        }
    }

    @Data
    @NoArgsConstructor
    public static class JellyfishSighting {
        private String species;
        private String commonName;
        private String description;
        private LocalDateTime observedDate;
        private double latitude;
        private double longitude;
        private String reportedBy;
        private boolean verified;
        private SeverityLevel severity;
        private double distanceKm;
        private int daysAgo;

        public enum SeverityLevel {
            HARMLESS("Harmless", "🟢"),
            MILD("Mild sting", "🟡"),
            PAINFUL("Painful sting", "🟠"),
            DANGEROUS("Dangerous", "🔴"),
            EXTREME("Life-threatening", "⚫");

            private final String description;
            private final String emoji;

            SeverityLevel(String description, String emoji) {
                this.description = description;
                this.emoji = emoji;
            }

            public String getDescription() {
                return description;
            }

            public String getEmoji() {
                return emoji;
            }

            public String getFormatted() {
                return emoji + " " + description;
            }
        }

        public String getFormattedDistance() {
            if (distanceKm < 1.0) {
                return String.format("%.0fm away", distanceKm * 1000);
            }
            return String.format("%.1fkm away", distanceKm);
        }

        public String getFormattedTimeAgo() {
            if (daysAgo == 0) {
                return "Today";
            } else if (daysAgo == 1) {
                return "Yesterday";
            } else {
                return daysAgo + " days ago";
            }
        }
    }

    public boolean hasRecentSightings() {
        return recentSightings != null && !recentSightings.isEmpty();
    }

    public String getFormattedRiskLevel() {
        return riskLevel != null ? riskLevel.getFormattedRisk() : "⚪ Unknown";
    }

    public String getFormattedAdvice() {
        if (safetyAdvice != null && !safetyAdvice.isEmpty()) {
            return safetyAdvice;
        }
        return getDefaultAdviceForRiskLevel();
    }

    private String getDefaultAdviceForRiskLevel() {
        if (riskLevel == null) return "Check local conditions before swimming";

        return switch (riskLevel) {
            case VERY_LOW -> "Perfect conditions for swimming! 🏊‍♂️";
            case LOW -> "Generally safe, but stay alert for jellyfish 👀";
            case MODERATE -> "Swimming possible, check water before entering ⚠️";
            case HIGH -> "Be very cautious, consider avoiding swimming 🚫";
            case VERY_HIGH -> "Do not swim! Dangerous jellyfish activity ⛔";
        };
    }

    public int getDangerousSightingsCount() {
        if (!hasRecentSightings()) return 0;

        return (int) recentSightings.stream()
                .filter(s -> s.getSeverity() == JellyfishSighting.SeverityLevel.DANGEROUS ||
                        s.getSeverity() == JellyfishSighting.SeverityLevel.EXTREME)
                .count();
    }

    public int getRecentSightingsCount() {
        if (!hasRecentSightings()) return 0;
        return (int) recentSightings.stream()
                .filter(s -> s.getDaysAgo() <= 7) // Last week
                .count();
    }
}