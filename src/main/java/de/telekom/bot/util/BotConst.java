package de.telekom.bot.util;

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

    public static final String WELCOME_MESSAGE =
            "🌊 *Welcome to Spanish Beach Bot!* 🏖️\n\n" +
                    "🏖️ *How to use the bot:*\n\n" +
                    "📝 **Step 1:** Simply type the name of any Spanish beach you're interested in\n" +
                    "🔍 **Step 2:** Get comprehensive information including:\n" +
                    "   • 🌡️ Current air & water temperature\n" +
                    "   • 🗺️ Exact coordinates & location on map\n" +
                    "   • 🌤️ Weather conditions & comfort level\n" +
                    "   • 🔍 Smart typo correction for beach names\n\n" +
                    "💬 *Example:* Just type \"Benidorm\" or \"Playa de la Concha\"\n\n" +
                    "📍 *Coverage areas:*\n" +
                    "   • Mediterranean coast (Costa Brava, Costa del Sol, Costa Blanca)\n" +
                    "   • Atlantic coast (Basque beaches, Galicia)\n" +
                    "   • Balearic Islands (Mallorca, Ibiza, Menorca)\n" +
                    "   • Canary Islands (Tenerife, Gran Canaria, Lanzarote)\n\n" +
                    "💡 *Tip:* Type /help to see example beach names!";

    public static final String HELP_MESSAGE =
            "🌊 *Spanish Beach Bot Help* 🏖️\n\n" +
                    "📝 **How to get beach information:**\n" +
                    "Just type the name of any Spanish beach!\n\n" +
                    "🔹 **Commands:**\n" +
                    "`/start` - Welcome message and instructions\n" +
                    "`/help` - Show this help message\n" +
                    "`/language` or `/lang` - Change bot language 🌍\n\n" +
                    "🏖️ **Popular beach examples to try:**\n\n" +
                    "**🌅 Costa Brava:**\n" +
                    "• Tossa de Mar • Lloret de Mar • Cadaqués\n\n" +
                    "**☀️ Costa del Sol:**\n" +
                    "• Marbella • Torremolinos • Nerja • Fuengirola\n\n" +
                    "**🏖️ Costa Blanca:**\n" +
                    "• Benidorm • Alicante • Calpe • Dénia\n\n" +
                    "**🏝️ Balearic Islands:**\n" +
                    "• Cala Comte • Es Trenc • Cala Turqueta • Playa de Palma\n\n" +
                    "**🌋 Basque Coast:**\n" +
                    "• La Concha • Zurriola • Sopelana\n\n" +
                    "💬 *Example:* Type \"Benidorm\" to get full information!";

}
