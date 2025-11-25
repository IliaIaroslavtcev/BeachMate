package de.telekom.bot.handler;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface CommandHandler {
    String getCommand();

    void handle(Update update, TelegramLongPollingBot bot) throws Exception;
}

