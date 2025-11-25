package de.telekom.bot.service;

import de.telekom.bot.util.BotConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Service for managing user language preferences
 */
@Service
public class UserLanguageService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserLanguageService.class);
    
    // In-memory storage for user language preferences
    // In production, this should be stored in a database
    private final Map<Long, String> userLanguages = new ConcurrentHashMap<>();
    
    /**
     * Get user's preferred language
     * @param chatId Telegram chat ID
     * @return Language code (en, es)
     */
    public String getUserLanguage(Long chatId) {
        String language = userLanguages.getOrDefault(chatId, BotConst.LANG_ENGLISH); // Default to English
        logger.debug("Retrieved language '{}' for chat ID: {}", language, chatId);
        return language;
    }
    
    /**
     * Set user's preferred language
     * @param chatId Telegram chat ID
     * @param languageCode Language code (en, es)
     */
    public void setUserLanguage(Long chatId, String languageCode) {
        if (isValidLanguage(languageCode)) {
            userLanguages.put(chatId, languageCode);
            logger.info("Set language '{}' for chat ID: {}", languageCode, chatId);
        } else {
            logger.warn("Attempted to set invalid language '{}' for chat ID: {}", languageCode, chatId);
        }
    }
    
    /**
     * Check if language code is supported
     * @param languageCode Language code to validate
     * @return true if language is supported
     */
    public boolean isValidLanguage(String languageCode) {
        return BotConst.LANG_ENGLISH.equals(languageCode) ||
               BotConst.LANG_SPANISH.equals(languageCode);
    }
    
    /**
     * Get language name with flag emoji
     * @param languageCode Language code
     * @return Formatted language name with flag
     */
    public String getLanguageDisplayName(String languageCode) {
        switch (languageCode) {
            case BotConst.LANG_ENGLISH:
                return BotConst.FLAG_ENGLISH + " English";
            case BotConst.LANG_SPANISH:
                return BotConst.FLAG_SPANISH + " Español";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Get all supported languages with their display names
     * @return Map of language codes to display names
     */
    public Map<String, String> getAllLanguages() {
        Map<String, String> languages = new ConcurrentHashMap<>();
        languages.put(BotConst.LANG_ENGLISH, getLanguageDisplayName(BotConst.LANG_ENGLISH));
        languages.put(BotConst.LANG_SPANISH, getLanguageDisplayName(BotConst.LANG_SPANISH));
        return languages;
    }
    
    /**
     * Get total number of users with language preferences
     * @return Number of users
     */
    public int getUserCount() {
        return userLanguages.size();
    }
    
    /**
     * Get statistics of language usage
     * @return Map of language codes to user counts
     */
    public Map<String, Long> getLanguageStats() {
        Map<String, Long> stats = new ConcurrentHashMap<>();
        for (String lang : userLanguages.values()) {
            stats.merge(lang, 1L, Long::sum);
        }
        return stats;
    }
}