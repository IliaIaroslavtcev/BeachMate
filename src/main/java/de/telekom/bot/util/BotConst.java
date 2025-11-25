package de.telekom.bot.util;

/**
 * Bot constants including commands, supported languages, and UI elements.
 */
public class BotConst {

    // Commands
    public static final String COMMAND_START = "/start";
    public static final String COMMAND_HELP = "/help";
    public static final String COMMAND_LANGUAGE = "/language";
    public static final String COMMAND_LANG = "/lang";
    
    // Supported languages
    public static final String LANG_ENGLISH = "en";
    public static final String LANG_SPANISH = "es";
    
    // Language flags for better UX
    public static final String FLAG_ENGLISH = "🇬🇧";
    public static final String FLAG_SPANISH = "🇪🇸";

    private BotConst() {
        // Utility class - prevent instantiation
    }
}
