package de.telekom.bot.handler;

import de.telekom.bot.service.UserLanguageService;
import de.telekom.bot.util.BotConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Short alias for LanguageCommandHandler
 * Handles /lang command which does the same as /language
 */
@Component
public class LangCommandHandler implements CommandHandler {
    
    @Autowired
    private LanguageCommandHandler languageCommandHandler;
    
    @Override
    public String getCommand() {
        return BotConst.COMMAND_LANG;
    }

    @Override
    public void handle(Update update, TelegramLongPollingBot bot) throws Exception {
        // Delegate to the full LanguageCommandHandler
        languageCommandHandler.handle(update, bot);
    }
}