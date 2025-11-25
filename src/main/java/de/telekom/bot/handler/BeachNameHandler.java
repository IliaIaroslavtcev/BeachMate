package de.telekom.bot.handler;

import de.telekom.bot.model.BeachLocation;
import de.telekom.bot.model.WeatherInfo;
import de.telekom.bot.service.BeachCharacteristicsService;
import de.telekom.bot.service.GeocodeService;
import de.telekom.bot.service.UserLanguageService;
import de.telekom.bot.service.WeatherService;
import de.telekom.bot.util.TypoCorrection;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class BeachNameHandler implements CommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(BeachNameHandler.class);

    private final GeocodeService geocodeService;
    private final WeatherService weatherService;
    private final BeachCharacteristicsService beachCharacteristicsService;
    private final UserLanguageService userLanguageService;

    @Override
    public String getCommand() {
        return "__BEACH_NAME__";
    }

    @Override
    public void handle(Update update, TelegramLongPollingBot bot) throws Exception {
        long chatId = update.getMessage().getChatId();
        String beachName = update.getMessage().getText().trim();
        
        // Get user's preferred language
        String userLanguage = userLanguageService.getUserLanguage(chatId);

        logger.info("Processing beach request for: {}", beachName);

        // Get beach coordinates using GeocodeService
        BeachLocation beachLocation = geocodeService.findBeachCoordinates(beachName);

        // Enhance with beach surface and characteristics if found
        if (beachLocation.isFound() && beachCharacteristicsService.isEnabled()) {
            try {
                beachCharacteristicsService.enhanceBeachLocation(beachLocation);
                logger.info("Enhanced beach with surface info: {}", beachLocation.getBeachSurface());
            } catch (Exception e) {
                logger.error("Failed to enhance beach characteristics", e);
            }
        }

        // If not found, check for typos and suggest corrections
        if (!beachLocation.isFound()) {
            TypoCorrection.TypoCorrectionSuggestion suggestion = TypoCorrection.findBestCorrection(beachName);
            if (suggestion != null) {
                logger.info("Suggesting typo correction: '{}' -> '{}'", beachName, suggestion.getSuggestedCorrection());
                sendTypoCorrectionMessage(chatId, suggestion, bot, userLanguage, update);
                return; // Don't send the "not found" message
            }
        }

        // Send "searching" notification if beach was found
        if (beachLocation.isFound()) {
            sendSearchingNotification(chatId, beachLocation, bot, userLanguage, update);
        }

        // Get weather information if beach was found
        WeatherInfo weatherInfo = null;
        if (beachLocation.isFound()) {
            try {
                if (weatherService.isEnabled()) {
                    weatherInfo = weatherService.getWeatherInfo(beachLocation);
                    logger.info("Weather info retrieved for {}: air={}°C, water={}°C",
                            beachLocation.getName(),
                            weatherInfo.getFormattedAirTemperature(),
                            weatherInfo.getFormattedWaterTemperature());
                } else {
                    // Use mock data for demonstration
                    weatherInfo = weatherService.getMockWeatherInfo(beachLocation);
                    logger.info("Using mock weather data for demonstration");
                }
            } catch (Exception e) {
                logger.error("Failed to get weather info for {}", beachLocation.getName(), e);
                weatherInfo = new WeatherInfo(); // Empty weather info
            }
        }

        // Format the beach information response with weather data
        String beachInfo = formatBeachInformation(beachName, beachLocation, weatherInfo, userLanguage);

        SendMessage message = new SendMessage(String.valueOf(chatId), beachInfo);
        message.setParseMode("Markdown");
        
        // Reply to the original message to keep the response in the same topic
        if (update.getMessage() != null) {
            message.setReplyToMessageId(update.getMessage().getMessageId());
        }

        // If beach was found, also send location message for easy verification
        if (beachLocation.isFound()) {
            sendLocationMessage(chatId, beachLocation, bot, update);
        }

        bot.execute(message);
    }

    private String formatBeachInformation(String beachName, BeachLocation location, WeatherInfo weatherInfo, String language) {
        if (!location.isFound()) {
            return formatBeachNotFound(beachName, language);
        }

        String locationHeader = location.getDisplayName() != null ?
                location.getDisplayName() : beachName;

        StringBuilder beachInfoBuilder = new StringBuilder();

        beachInfoBuilder.append(getBeachInfoHeader(language))
                .append(getLocationLabel(language)).append(locationHeader).append("\n\n")
                .append(getCoordinatesLabel(language))
                .append("• ").append(getLatitudeLabel(language)).append(String.format("%.6f", location.getLatitude())).append("`\n")
                .append("• ").append(getLongitudeLabel(language)).append(String.format("%.6f", location.getLongitude())).append("`\n\n")
                .append(getBeachTypeInfo(location, language));

        // Add weather information if available
        if (weatherInfo != null && weatherInfo.hasTemperatureData()) {
            beachInfoBuilder.append(language.equals("es") ? "🌡️ **Condiciones Actuales:**\n" : "🌡️ **Current Conditions:**\n");

            if (weatherInfo.isAirTempFound()) {
                beachInfoBuilder.append(language.equals("es") ? "• Temperatura del Aire: **" : "• Air Temperature: **")
                        .append(weatherInfo.getFormattedAirTemperature())
                        .append("**\n");
            }

            if (weatherInfo.isWaterTempFound()) {
                beachInfoBuilder.append(language.equals("es") ? "• Temperatura del Agua: **" : "• Water Temperature: **")
                        .append(weatherInfo.getFormattedWaterTemperature())
                        .append("**\n");
            }

            if (weatherInfo.getDescription() != null) {
                beachInfoBuilder.append(language.equals("es") ? "• Clima: " : "• Weather: ")
                        .append(weatherInfo.getDescription()).append("\n");
            }

            if (weatherInfo.getHumidity() != null) {
                beachInfoBuilder.append(language.equals("es") ? "• Humedad: " : "• Humidity: ")
                        .append(weatherInfo.getHumidity()).append("%\n");
            }

            if (weatherInfo.getWindSpeed() != null) {
                beachInfoBuilder.append(language.equals("es") ? "• Viento: " : "• Wind: ")
                        .append(String.format("%.1f m/s", weatherInfo.getWindSpeed())).append("\n");
            }

            beachInfoBuilder.append("\n")
                    .append(language.equals("es") ? "🌈 **Nivel de Confort:** " : "🌈 **Comfort Level:** ")
                    .append(weatherInfo.getComfortDescription())
                    .append("\n\n");

            // Add detailed jellyfish information if available
            if (weatherInfo.getJellyfishInfo() != null) {
                beachInfoBuilder.append(formatJellyfishInfo(weatherInfo.getJellyfishInfo(), language));
            }
        } else {
            beachInfoBuilder.append(language.equals("es") ? "🌡️ **Datos meteorológicos no disponibles actualmente**\n\n" : "🌡️ **Weather data currently unavailable**\n\n");
        }

        beachInfoBuilder.append(formatLocalAttractions(location, language))
                .append(formatDataSources(weatherInfo, language));
        
        if (language.equals("es")) {
            beachInfoBuilder.append("💡 *¡Prueba escribir otro nombre de playa o usa /help para ver ejemplos!*");
        } else {
            beachInfoBuilder.append("💡 *Try typing another beach name or use /help for examples!*");
        }

        return beachInfoBuilder.toString();
    }

    private String formatBeachNotFound(String beachName, String language) {
        return switch (language) {
            case "en" -> "🏖️ *Search Results* 🔍\n\n" +
                    "❌ **Beach not found:** " + beachName + "\n\n" +
                    "🤔 **Possible reasons:**\n" +
                    "• The beach name might be misspelled\n" +
                    "• The location might not be in Spain\n" +
                    "• The beach might not be in our database yet\n\n" +
                    "💡 **Try these suggestions:**\n" +
                    "• Check the spelling of the beach name\n" +
                    "• Include the city or region (e.g., \"Benidorm\")\n" +
                    "• Use Spanish names (e.g., \"Playa de la Concha\")\n" +
                    "• Type /help for popular beach examples\n\n" +
                    "🔄 *We're constantly expanding our database of Spanish beaches!*";
                    
            case "es" -> "🏖️ *Resultados de Búsqueda* 🔍\n\n" +
                    "❌ **Playa no encontrada:** " + beachName + "\n\n" +
                    "🤔 **Posibles razones:**\n" +
                    "• El nombre de la playa puede estar mal escrito\n" +
                    "• La ubicación puede no estar en España\n" +
                    "• La playa puede no estar en nuestra base de datos aún\n\n" +
                    "💡 **Prueba estas sugerencias:**\n" +
                    "• Verifica la ortografía del nombre de la playa\n" +
                    "• Incluye la ciudad o región (ej., \"Benidorm\")\n" +
                    "• Usa nombres en español (ej., \"Playa de la Concha\")\n" +
                    "• Escribe /help para ejemplos de playas populares\n\n" +
                    "🔄 *¡Estamos expandiendo constantemente nuestra base de datos de playas españolas!*";
                    
            default -> "🏖️ *Search Results* 🔍\n\n" +
                    "❌ **Beach not found:** " + beachName + "\n\n" +
                    "🤔 **Possible reasons:**\n" +
                    "• The beach name might be misspelled\n" +
                    "• The location might not be in Spain\n" +
                    "• The beach might not be in our database yet\n\n" +
                    "💡 **Try these suggestions:**\n" +
                    "• Check the spelling of the beach name\n" +
                    "• Include the city or region (e.g., \"Benidorm\")\n" +
                    "• Use Spanish names (e.g., \"Playa de la Concha\")\n" +
                    "• Type /help for popular beach examples\n\n" +
                    "🔄 *We're constantly expanding our database of Spanish beaches!*";
        };
    }

    private String getBeachTypeInfo(BeachLocation location, String language) {
        StringBuilder typeInfo = new StringBuilder();

        if (location.isBeach()) {
            typeInfo.append(getBeachTypeConfirmed(language));

            // Add surface information if available
            if (location.getBeachSurface() != null && !location.getBeachSurface().isEmpty()) {
                String surfaceEmoji = getSurfaceEmoji(location.getBeachSurface());
                typeInfo.append("\n").append(surfaceEmoji).append(" **").append(getSurfaceLabel(language)).append("** ")
                        .append(location.getBeachSurface()).append(" ").append(getBeachWord(language));
            }

            typeInfo.append("\n\n");
        } else {
            typeInfo.append(getCoastalLocationInfo(location, language));
        }

        return typeInfo.toString();
    }

    /**
     * Format local attractions and area information
     */
    private String formatLocalAttractions(BeachLocation location, String language) {
        if (location == null || location.getName() == null) {
            return "";
        }
        
        StringBuilder attractions = new StringBuilder();
        attractions.append(language.equals("es") ? "🏺 **Acerca de esta área:**\n" : "🏺 **About this area:**\n");
        
        String locationName = location.getName().toLowerCase();
        String displayName = location.getDisplayName() != null ? location.getDisplayName().toLowerCase() : "";
        
        // Generate location-specific attractions info
        String attractionInfo = getAttractionInfo(locationName, displayName);
        
        if (!attractionInfo.isEmpty()) {
            attractions.append(attractionInfo).append("\n\n");
        } else {
            // Generic fallback
            attractions.append("This beautiful Spanish beach offers great opportunities for swimming, sunbathing, and enjoying the Mediterranean coastline.\n\n");
        }
        
        return attractions.toString();
    }
    
    /**
     * Get specific attraction information for known locations
     */
    private String getAttractionInfo(String locationName, String displayName) {
        // Benidorm
        if (locationName.contains("benidorm") || displayName.contains("benidorm")) {
            return "Benidorm is famous for its towering skyscrapers, vibrant nightlife, and two main beaches. The old town offers charming tapas bars, while the nearby Terra Mítica theme park provides family entertainment. Don't miss the scenic Balcón del Mediterráneo viewpoint.";
        }
        
        // Valencia
        if (locationName.contains("valencia") || displayName.contains("valencia")) {
            return "Valencia combines beautiful beaches with rich culture and history. Visit the stunning City of Arts and Sciences, explore the historic Central Market, and try the authentic paella valenciana in its birthplace. The nearby Albufera Natural Park offers scenic boat trips.";
        }
        
        // Barcelona
        if (locationName.contains("barcelona") || displayName.contains("barcelona")) {
            return "Barcelona's beaches provide urban seaside relaxation near world-class attractions. The Gothic Quarter, Sagrada Família, and Park Güell are must-sees, while the bustling Las Ramblas offers shopping and dining. The beachfront Barceloneta district is perfect for seafood.";
        }
        
        // Marbella
        if (locationName.contains("marbella") || displayName.contains("marbella")) {
            return "Marbella epitomizes Costa del Sol glamour with luxury marinas, upscale shopping, and golden beaches. Puerto Banús marina showcases superyachts and designer boutiques, while the charming old town features whitewashed buildings and traditional Andalusian architecture.";
        }
        
        // San Sebastián / Donostia
        if (locationName.contains("san sebastian") || locationName.contains("donostia") || 
            displayName.contains("san sebastián") || displayName.contains("donostia")) {
            return "San Sebastián is a culinary paradise with more Michelin stars per capita than anywhere else. The beautiful La Concha beach sits in a perfect shell-shaped bay, while the old town offers incredible pintxos bars. Mount Urgull provides panoramic city views.";
        }
        
        // Palma de Mallorca
        if (locationName.contains("palma") || displayName.contains("palma") || displayName.contains("mallorca")) {
            return "Palma combines stunning beaches with impressive architecture, including the magnificent Gothic cathedral La Seu. The historic old town features narrow streets with boutiques and cafés, while the nearby Tramuntana mountains offer hiking and scenic drives.";
        }
        
        // Santander
        if (locationName.contains("santander") || displayName.contains("santander")) {
            return "Santander offers beautiful beaches and elegant architecture along the Cantabrian coast. The Palacio de la Magdalena provides royal history and gardens, while the nearby Picos de Europa mountains offer stunning natural landscapes just a short drive away.";
        }
        
        // Alicante
        if (locationName.contains("alicante") || displayName.contains("alicante")) {
            return "Alicante features excellent beaches beneath the impressive Santa Bárbara Castle, which offers panoramic coastal views. The charming old town Barrio Santa Cruz has colorful houses and narrow streets, while the palm-lined Explanada de España is perfect for evening strolls.";
        }
        
        // Málaga
        if (locationName.contains("malaga") || locationName.contains("málaga") || 
            displayName.contains("malaga") || displayName.contains("málaga")) {
            return "Málaga beautifully blends beach relaxation with cultural richness as Picasso's birthplace. The historic Alcazaba fortress and Roman theatre showcase ancient history, while modern attractions include the Picasso Museum and the vibrant Soho arts district.";
        }
        
        // Tossa de Mar
        if (locationName.contains("tossa") || displayName.contains("tossa")) {
            return "Tossa de Mar is a picturesque Costa Brava gem with a perfectly preserved medieval old town perched above crystalline coves. The ancient walls and towers create a romantic atmosphere, while hidden beaches and scenic coastal paths offer natural beauty.";
        }
        
        // Nerja
        if (locationName.contains("nerja") || displayName.contains("nerja")) {
            return "Nerja is famous for its spectacular Balcón de Europa viewpoint and the impressive Nerja Caves with ancient paintings. This charming white village offers beautiful coves, traditional Spanish atmosphere, and stunning mountain backdrops perfect for photography.";
        }
        
        // Canary Islands - Las Palmas
        if (locationName.contains("las palmas") || locationName.contains("canteras") || 
            displayName.contains("las palmas") || displayName.contains("gran canaria")) {
            return "Las Palmas offers year-round perfect weather with the stunning Las Canteras beach stretching for miles. The historic Vegueta district features Columbus connections and colonial architecture, while the modern city provides excellent shopping and dining.";
        }
        
        // Regional fallbacks
        if (displayName.contains("costa brava") || displayName.contains("girona")) {
            return "The Costa Brava offers dramatic clifftop views, hidden coves, and charming fishing villages. This rugged coastline features crystal-clear waters perfect for snorkeling, while nearby medieval towns like Besalú and Girona provide rich cultural experiences.";
        }
        
        if (displayName.contains("costa del sol")) {
            return "The Costa del Sol enjoys over 300 days of sunshine annually, making it perfect for beach lovers. Traditional white villages dot the nearby mountains, while golf courses, marinas, and vibrant nightlife cater to every taste along this famous coastline.";
        }
        
        if (displayName.contains("costa blanca")) {
            return "The Costa Blanca features fine sandy beaches backed by dramatic mountain ranges. Charming towns with traditional Spanish architecture, excellent local markets, and authentic cuisine make this region perfect for experiencing authentic Mediterranean culture.";
        }
        
        // Default empty - will use generic fallback
        return "";
    }
    
    /**
     * Format consolidated data sources information
     */
    private String formatDataSources(WeatherInfo weatherInfo, String language) {
        StringBuilder sources = new StringBuilder();
        sources.append("\n");
        sources.append(language.equals("es") ? "_Fuentes: " : "_Sources: ");
        
        boolean first = true;
        
        // Weather data source
        if (weatherInfo != null && weatherInfo.getSource() != null) {
            if (!first) sources.append(", ");
            sources.append(weatherInfo.getSource());
            first = false;
        }
        
        // Jellyfish data source
        if (weatherInfo != null && weatherInfo.getJellyfishInfo() != null && 
            weatherInfo.getJellyfishInfo().getSource() != null) {
            if (!first) sources.append(", ");
            sources.append(weatherInfo.getJellyfishInfo().getSource());
            first = false;
        }
        
        // Beach characteristics
        if (!first) sources.append(", ");
        sources.append("OpenStreetMap_\n\n");
        
        return sources.toString();
    }
    
    /**
     * Get emoji for beach surface type
     */
    private String getSurfaceEmoji(String surface) {
        if (surface == null) return "🏖️";

        return switch (surface.toLowerCase()) {
            case "sand", "sandy" -> "🏖️";
            case "pebble", "pebbles" -> "🪨";
            case "rocky", "rocks", "stone" -> "🪨";
            case "gravel" -> "🪨";
            case "mixed" -> "🌊";
            case "artificial" -> "🏗️";
            default -> "🏖️";
        };
    }

    /**
     * Send searching notification to show progress
     */
    private void sendSearchingNotification(long chatId, BeachLocation location, TelegramLongPollingBot bot, String language, Update originalUpdate) {
        try {
            String searchingMessage = formatSearchingMessage(location, language);

            SendMessage message = new SendMessage(String.valueOf(chatId), searchingMessage);
            message.setParseMode("Markdown");
            
            // Reply to the original message to keep the response in the same topic
            if (originalUpdate.getMessage() != null) {
                message.setReplyToMessageId(originalUpdate.getMessage().getMessageId());
            }

            bot.execute(message);
            logger.info("Sent searching notification for: {}", location.getName());

        } catch (Exception e) {
            logger.error("Error sending searching notification", e);
        }
    }

    /**
     * Format searching progress message
     */
    private String formatSearchingMessage(BeachLocation location, String language) {
        String locationName = location.getDisplayName() != null ? location.getDisplayName() : location.getName();
        
        return switch (language) {
            case "en" -> "🔍 *Searching for beach information...* \n\n" +
                    "📍 **Location:** " + locationName + "\n\n" +
                    "🌡️ Getting weather data...\n" +
                    "🪼 Checking jellyfish activity...\n" +
                    "🏖️ Analyzing beach surface...\n" +
                    "🌊 Analyzing marine conditions...\n\n" +
                    "⏳ *Please wait, this may take a few seconds...*";
                    
            case "es" -> "🔍 *Buscando información de la playa...* \n\n" +
                    "📍 **Ubicación:** " + locationName + "\n\n" +
                    "🌡️ Obteniendo datos meteorológicos...\n" +
                    "🪼 Verificando actividad de medusas...\n" +
                    "🏖️ Analizando superficie de la playa...\n" +
                    "🌊 Analizando condiciones marinas...\n\n" +
                    "⏳ *Espera un momento, esto puede tomar unos segundos...*";
                    
            default -> "🔍 *Searching for beach information...* \n\n" +
                    "📍 **Location:** " + locationName + "\n\n" +
                    "🌡️ Getting weather data...\n" +
                    "🪼 Checking jellyfish activity...\n" +
                    "🏖️ Analyzing beach surface...\n" +
                    "🌊 Analyzing marine conditions...\n\n" +
                    "⏳ *Please wait, this may take a few seconds...*";
        };
    }

    /**
     * Send location message with beach coordinates
     */
    private void sendLocationMessage(long chatId, BeachLocation beach, TelegramLongPollingBot bot, Update originalUpdate) {
        try {
            org.telegram.telegrambots.meta.api.methods.send.SendLocation location =
                    new org.telegram.telegrambots.meta.api.methods.send.SendLocation();

            location.setChatId(String.valueOf(chatId));
            location.setLatitude(beach.getLatitude());
            location.setLongitude(beach.getLongitude());
            location.setHorizontalAccuracy(50.0); // 50 meter accuracy
            
            // Reply to the original message to keep the response in the same topic
            if (originalUpdate.getMessage() != null) {
                location.setReplyToMessageId(originalUpdate.getMessage().getMessageId());
            }

            bot.execute(location);
            logger.info("Sent location for beach {}", beach.getName());
        } catch (Exception e) {
            logger.error("Error sending location", e);
        }
    }

    /**
     * Send typo correction message with suggestion
     */
    private void sendTypoCorrectionMessage(long chatId, TypoCorrection.TypoCorrectionSuggestion suggestion,
                                           TelegramLongPollingBot bot, String language, Update originalUpdate) {
        try {
            String correctionMessage = formatTypoCorrectionMessage(suggestion, language);

            SendMessage message = new SendMessage(String.valueOf(chatId), correctionMessage);
            message.setParseMode("Markdown");
            
            // Reply to the original message to keep the response in the same topic
            if (originalUpdate.getMessage() != null) {
                message.setReplyToMessageId(originalUpdate.getMessage().getMessageId());
            }

            bot.execute(message);
            logger.info("Sent typo correction suggestion for: {}", suggestion.getOriginalInput());

        } catch (Exception e) {
            logger.error("Error sending typo correction message", e);
        }
    }

    /**
     * Format detailed jellyfish information
     */
    private String formatJellyfishInfo(de.telekom.bot.model.JellyfishInfo jellyfishInfo, String language) {
        StringBuilder jellyfishSection = new StringBuilder();

        jellyfishSection.append(language.equals("es") ? "🪼 **Alerta de Seguridad de Medusas:**\n" : "🪼 **Jellyfish Safety Alert:**\n");

        // Risk level with emoji
        String riskEmoji = switch (jellyfishInfo.getRiskLevel()) {
            case VERY_HIGH -> "🚨";
            case HIGH -> "⚠️";
            case MODERATE -> "🟡";
            case LOW -> "🟢";
            case VERY_LOW -> "✅";
        };

        jellyfishSection.append(language.equals("es") ? "• Nivel de Riesgo: " : "• Risk Level: ")
                .append(riskEmoji)
                .append(" **").append(jellyfishInfo.getRiskLevel().getDisplayName())
                .append("**\n");

        if (jellyfishInfo.getPrediction() != null) {
            jellyfishSection.append(language.equals("es") ? "• Estado: " : "• Status: ")
                    .append(jellyfishInfo.getPrediction()).append("\n");
        }

        if (jellyfishInfo.getSafetyAdvice() != null) {
            jellyfishSection.append(language.equals("es") ? "• Consejo: " : "• Advice: ")
                    .append(jellyfishInfo.getSafetyAdvice()).append("\n");
        }

        // Show recent sightings if any (only meaningful ones)
        if (jellyfishInfo.getRecentSightings() != null && !jellyfishInfo.getRecentSightings().isEmpty()) {
            // Filter out unknown/poor quality sightings for display
            var meaningfulSightings = jellyfishInfo.getRecentSightings().stream()
                    .filter(s -> s.getCommonName() != null && !s.getCommonName().equals("Unknown"))
                    .filter(s -> s.getDaysAgo() >= 0) // Valid date
                    .limit(3)
                    .toList();

            if (!meaningfulSightings.isEmpty()) {
                jellyfishSection.append(language.equals("es") ? "\n📊 **Actividad Reciente:**\n" : "\n📊 **Recent Activity:**\n");

                for (var sighting : meaningfulSightings) {
                    String severityEmoji = switch (sighting.getSeverity()) {
                        case EXTREME -> "🚨";
                        case DANGEROUS -> "⚠️";
                        case PAINFUL -> "😰";
                        case MILD -> "🟡";
                        default -> "🟡";
                    };

                    String timeText;
                    if (language.equals("es")) {
                        timeText = sighting.getDaysAgo() == 0 ? "hoy" :
                                sighting.getDaysAgo() == 1 ? "ayer" :
                                        "hace " + sighting.getDaysAgo() + " días";
                    } else {
                        timeText = sighting.getDaysAgo() == 0 ? "today" :
                                sighting.getDaysAgo() == 1 ? "yesterday" :
                                        sighting.getDaysAgo() + " days ago";
                    }

                    jellyfishSection.append("• ").append(severityEmoji)
                            .append(" ").append(sighting.getCommonName())
                            .append(" (").append(timeText).append(", ")
                            .append(String.format("%.1f", sighting.getDistanceKm())).append(" km)\n");
                }

                // Show total count if there are more
                int totalSightings = jellyfishInfo.getRecentSightings().size();
                if (totalSightings > meaningfulSightings.size()) {
                    int additionalCount = totalSightings - meaningfulSightings.size();
                    if (language.equals("es")) {
                        jellyfishSection.append("• ... y ").append(additionalCount)
                                .append(" observación").append(additionalCount > 1 ? "es" : "")
                                .append(" más en el área\n");
                    } else {
                        jellyfishSection.append("• ... and ").append(additionalCount)
                                .append(" more observation").append(additionalCount > 1 ? "s" : "")
                                .append(" in the area\n");
                    }
                }
            }
        }
        
        jellyfishSection.append("\n");
        return jellyfishSection.toString();
    }

    /**
     * Format typo correction message
     */
    private String formatTypoCorrectionMessage(TypoCorrection.TypoCorrectionSuggestion suggestion, String language) {
        return switch (language) {
            case "en" -> "🤔 *Hmm, location not found...* \n\n" +
                    "❓ **You searched for:** `" + suggestion.getOriginalInput() + "`\n\n" +
                    "💡 **Did you mean:** *" + suggestion.getCapitalizedSuggestion() + "*?\n\n" +
                    "🔄 **Try typing:** `" + suggestion.getSuggestedCorrection() + "`\n\n" +
                    "✨ **Why this suggestion?**\n" +
                    "• It's a popular Spanish beach destination\n" +
                    "• Only " + suggestion.getEditDistance() + " character(s) different from your input\n" +
                    "• Many travelers search for this location\n\n" +
                    "🏖️ **Other popular destinations to try:**\n" +
                    "• `benidorm` - Famous Costa Blanca resort\n" +
                    "• `valencia` - Beautiful Mediterranean beaches\n" +
                    "• `alicante` - Historic coastal city\n" +
                    "• `marbella` - Glamorous Costa del Sol\n" +
                    "• `barcelona` - Catalonian beach culture\n\n" +
                    "💬 *Just type the corrected name to search for beaches!*";
                    
            case "es" -> "🤔 *Hmm, ubicación no encontrada...* \n\n" +
                    "❓ **Buscaste:** `" + suggestion.getOriginalInput() + "`\n\n" +
                    "💡 **¿Querías decir:** *" + suggestion.getCapitalizedSuggestion() + "*?\n\n" +
                    "🔄 **Prueba escribiendo:** `" + suggestion.getSuggestedCorrection() + "`\n\n" +
                    "✨ **¿Por qué esta sugerencia?**\n" +
                    "• Es un destino de playa popular en España\n" +
                    "• Solo " + suggestion.getEditDistance() + " caracter(es) diferentes de tu entrada\n" +
                    "• Muchos viajeros buscan esta ubicación\n\n" +
                    "🏖️ **Otros destinos populares para probar:**\n" +
                    "• `benidorm` - Famoso resort de Costa Blanca\n" +
                    "• `valencia` - Hermosas playas mediterráneas\n" +
                    "• `alicante` - Ciudad costera histórica\n" +
                    "• `marbella` - Glamurosa Costa del Sol\n" +
                    "• `barcelona` - Cultura playera catalana\n\n" +
                    "💬 *¡Simplemente escribe el nombre corregido para buscar playas!*";
                    
            default -> "🤔 *Hmm, location not found...* \n\n" +
                    "❓ **You searched for:** `" + suggestion.getOriginalInput() + "`\n\n" +
                    "💡 **Did you mean:** *" + suggestion.getCapitalizedSuggestion() + "*?\n\n" +
                    "🔄 **Try typing:** `" + suggestion.getSuggestedCorrection() + "`\n\n" +
                    "✨ **Why this suggestion?**\n" +
                    "• It's a popular Spanish beach destination\n" +
                    "• Only " + suggestion.getEditDistance() + " character(s) different from your input\n" +
                    "• Many travelers search for this location\n\n" +
                    "🏖️ **Other popular destinations to try:**\n" +
                    "• `benidorm` - Famous Costa Blanca resort\n" +
                    "• `valencia` - Beautiful Mediterranean beaches\n" +
                    "• `alicante` - Historic coastal city\n" +
                    "• `marbella` - Glamorous Costa del Sol\n" +
                    "• `barcelona` - Catalonian beach culture\n\n" +
                    "💬 *Just type the corrected name to search for beaches!*";
        };
    }
    
    // Localization helper methods
    private String getBeachInfoHeader(String language) {
        return switch (language) {
            case "en" -> "🏖️ *Beach Information* 🌊\n\n";
            case "es" -> "🏖️ *Información de Playa* 🌊\n\n";
            default -> "🏖️ *Beach Information* 🌊\n\n"; // Default to English
        };
    }
    
    private String getLocationLabel(String language) {
        return switch (language) {
            case "en" -> "📍 **Location:** ";
            case "es" -> "📍 **Ubicación:** ";
            default -> "📍 **Location:** "; // Default to English
        };
    }
    
    private String getCoordinatesLabel(String language) {
        return switch (language) {
            case "en" -> "🗺️ **Coordinates:**\n";
            case "es" -> "🗺️ **Coordenadas:**\n";
            default -> "🗺️ **Coordinates:**\n"; // Default to English
        };
    }
    
    private String getLatitudeLabel(String language) {
        return switch (language) {
            case "en" -> "Latitude: `";
            case "es" -> "Latitud: `";
            default -> "Latitude: `"; // Default to English
        };
    }
    
    private String getLongitudeLabel(String language) {
        return switch (language) {
            case "en" -> "Longitude: `";
            case "es" -> "Longitud: `";
            default -> "Longitude: `"; // Default to English
        };
    }
    
    private String getBeachTypeConfirmed(String language) {
        return switch (language) {
            case "en" -> "✅ **Type:** Confirmed beach location";
            case "es" -> "✅ **Tipo:** Ubicación de playa confirmada";
            default -> "✅ **Type:** Confirmed beach location"; // Default to English
        };
    }
    
    private String getSurfaceLabel(String language) {
        return switch (language) {
            case "en" -> "Surface:";
            case "es" -> "Superficie:";
            default -> "Surface:"; // Default to English
        };
    }
    
    private String getBeachWord(String language) {
        return switch (language) {
            case "en" -> "beach";
            case "es" -> "playa";
            default -> "beach"; // Default to English
        };
    }
    
    private String getCoastalLocationInfo(BeachLocation location, String language) {
        String typeLabel = switch (language) {
            case "en" -> "Type:";
            case "es" -> "Tipo:";
            default -> "Type:"; // Default to English
        };
        
        String coastalLocation = switch (language) {
            case "en" -> "Coastal location";
            case "es" -> "Ubicación costera";
            default -> "Coastal location"; // Default to English
        };
        
        return "📍 **" + typeLabel + "** " + coastalLocation + " (" + location.getType() + ")\n\n";
    }
}
