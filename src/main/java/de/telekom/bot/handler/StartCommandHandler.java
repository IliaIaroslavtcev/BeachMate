package de.telekom.bot.handler;

import de.telekom.bot.service.UserLanguageService;
import de.telekom.bot.util.BotConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class StartCommandHandler implements CommandHandler {
    
    @Autowired
    private UserLanguageService userLanguageService;
    
    @Override
    public String getCommand() {
        return BotConst.COMMAND_START;
    }

    @Override
    public void handle(Update update, TelegramLongPollingBot bot) throws Exception {
        long chatId = update.getMessage().getChatId();
        
        // Get user's preferred language
        String userLanguage = userLanguageService.getUserLanguage(chatId);
        
        // Get localized welcome message
        String welcomeMessage = getWelcomeMessage(userLanguage);
        
        SendMessage message = new SendMessage(String.valueOf(chatId), welcomeMessage);
        message.setParseMode("Markdown");
        
        // Reply to the original message to keep the response in the same topic
        if (update.getMessage() != null) {
            message.setReplyToMessageId(update.getMessage().getMessageId());
        }

        bot.execute(message);
    }
    
    private String getWelcomeMessage(String language) {
        switch (language) {
            case BotConst.LANG_ENGLISH:
                return "🌊 *Welcome to Spanish Beach Bot!* 🏖️\n\n" +
                       "🏖️ *How to use the bot:*\n\n" +
                       "📝 **Step 1:** Simply type the name of any Spanish beach you're interested in\n" +
                       "🔍 **Step 2:** Get comprehensive information including:\n" +
                       "   • 🌡️ Current air & water temperature\n" +
                       "   • 🗺️ Exact coordinates & location on map\n" +
                       "   • 🌤️ Weather conditions & comfort level\n" +
                       "   • 🪼 Jellyfish safety alerts\n" +
                       "   • 🏖️ Beach surface information\n\n" +
                       "💬 *Example:* Just type \"Benidorm\" or \"Playa de la Concha\"\n\n" +
                       "📍 *Coverage areas:*\n" +
                       "   • Mediterranean coast (Costa Brava, Costa del Sol, Costa Blanca)\n" +
                       "   • Atlantic coast (Basque beaches, Galicia)\n" +
                       "   • Balearic Islands (Mallorca, Ibiza, Menorca)\n" +
                       "   • Canary Islands (Tenerife, Gran Canaria, Lanzarote)\n\n" +
                       "🌍 *Change language:* /language\n" +
                       "💡 *Tip:* Type /help to see example beach names!";
                       
            case BotConst.LANG_SPANISH:
                return "🌊 *¡Bienvenido al Bot de Playas Españolas!* 🏖️\n\n" +
                       "🏖️ *Cómo usar el bot:*\n\n" +
                       "📝 **Paso 1:** Simplemente escribe el nombre de cualquier playa española que te interese\n" +
                       "🔍 **Paso 2:** Obtén información completa incluyendo:\n" +
                       "   • 🌡️ Temperatura actual del aire y del agua\n" +
                       "   • 🗺️ Coordenadas exactas y ubicación en el mapa\n" +
                       "   • 🌤️ Condiciones meteorológicas y nivel de confort\n" +
                       "   • 🪼 Alertas de seguridad sobre medusas\n" +
                       "   • 🏖️ Información de la superficie de la playa\n\n" +
                       "💬 *Ejemplo:* Simplemente escribe \"Benidorm\" o \"Playa de la Concha\"\n\n" +
                       "📍 *Áreas de cobertura:*\n" +
                       "   • Costa mediterránea (Costa Brava, Costa del Sol, Costa Blanca)\n" +
                       "   • Costa atlántica (playas vascas, Galicia)\n" +
                       "   • Islas Baleares (Mallorca, Ibiza, Menorca)\n" +
                       "   • Islas Canarias (Tenerife, Gran Canaria, Lanzarote)\n\n" +
                       "🌍 *Cambiar idioma:* /language\n" +
                       "💡 *Consejo:* Escribe /help para ver ejemplos de nombres de playas!";
                       
            default:
                // Default to English if language is not recognized
                return "🌊 *Welcome to Spanish Beach Bot!* 🏖️\n\n" +
                       "🏖️ *How to use the bot:*\n\n" +
                       "📝 **Step 1:** Simply type the name of any Spanish beach you're interested in\n" +
                       "🔍 **Step 2:** Get comprehensive information including:\n" +
                       "   • 🌡️ Current air & water temperature\n" +
                       "   • 🗺️ Exact coordinates & location on map\n" +
                       "   • 🌤️ Weather conditions & comfort level\n" +
                       "   • 🪼 Jellyfish safety alerts\n" +
                       "   • 🏖️ Beach surface information\n\n" +
                       "💬 *Example:* Just type \"Benidorm\" or \"Playa de la Concha\"\n\n" +
                       "📍 *Coverage areas:*\n" +
                       "   • Mediterranean coast (Costa Brava, Costa del Sol, Costa Blanca)\n" +
                       "   • Atlantic coast (Basque beaches, Galicia)\n" +
                       "   • Balearic Islands (Mallorca, Ibiza, Menorca)\n" +
                       "   • Canary Islands (Tenerife, Gran Canaria, Lanzarote)\n\n" +
                       "🌍 *Change language:* /language\n" +
                       "💡 *Tip:* Type /help to see example beach names!";
        }
    }
}

