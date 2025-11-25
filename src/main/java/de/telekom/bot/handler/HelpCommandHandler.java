package de.telekom.bot.handler;

import de.telekom.bot.service.UserLanguageService;
import de.telekom.bot.util.BotConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class HelpCommandHandler implements CommandHandler {
    
    @Autowired
    private UserLanguageService userLanguageService;
    
    @Override
    public String getCommand() {
        return BotConst.COMMAND_HELP;
    }

    @Override
    public void handle(Update update, TelegramLongPollingBot bot) throws Exception {
        long chatId = update.getMessage().getChatId();
        
        // Get user's preferred language
        String userLanguage = userLanguageService.getUserLanguage(chatId);
        
        // Get localized help message
        String helpMessage = getHelpMessage(userLanguage);
        
        SendMessage message = new SendMessage(String.valueOf(chatId), helpMessage);
        message.setParseMode("Markdown");
        
        // Reply to the original message to keep the response in the same topic
        if (update.getMessage() != null) {
            message.setReplyToMessageId(update.getMessage().getMessageId());
        }

        bot.execute(message);
    }
    
    private String getHelpMessage(String language) {
        switch (language) {
            case BotConst.LANG_ENGLISH:
                return "🌊 *Spanish Beach Bot Help* 🏖️\n\n" +
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
                       
            case BotConst.LANG_SPANISH:
                return "🌊 *Ayuda del Bot de Playas Españolas* 🏖️\n\n" +
                       "📝 **Cómo obtener información de playas:**\n" +
                       "¡Simplemente escribe el nombre de cualquier playa española!\n\n" +
                       "🔹 **Comandos:**\n" +
                       "`/start` - Mensaje de bienvenida e instrucciones\n" +
                       "`/help` - Mostrar este mensaje de ayuda\n" +
                       "`/language` o `/lang` - Cambiar idioma del bot 🌍\n\n" +
                       "🏖️ **Ejemplos de playas populares para probar:**\n\n" +
                       "**🌅 Costa Brava:**\n" +
                       "• Tossa de Mar • Lloret de Mar • Cadaqués\n\n" +
                       "**☀️ Costa del Sol:**\n" +
                       "• Marbella • Torremolinos • Nerja • Fuengirola\n\n" +
                       "**🏖️ Costa Blanca:**\n" +
                       "• Benidorm • Alicante • Calpe • Dénia\n\n" +
                       "**🏝️ Islas Baleares:**\n" +
                       "• Cala Comte • Es Trenc • Cala Turqueta • Playa de Palma\n\n" +
                       "**🌋 Costa Vasca:**\n" +
                       "• La Concha • Zurriola • Sopelana\n\n" +
                       "💬 *Ejemplo:* ¡Escribe \"Benidorm\" para obtener información completa!";
                       
            default:
                return BotConst.HELP_MESSAGE;
        }
    }
}
