package de.telekom.bot.service;

import de.telekom.bot.handler.CommandHandler;
import de.telekom.bot.handler.UnknownCommandHandler;
import de.telekom.bot.handler.BeachNameHandler;
import de.telekom.bot.handler.LanguageCommandHandler;
import de.telekom.bot.util.BotConst;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommandDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(CommandDispatcher.class);

    private final Map<String, CommandHandler> commandHandlers = new HashMap<>();
    private final UnknownCommandHandler unknownCommandHandler;
    private final BeachNameHandler beachNameHandler;
    private final LanguageCommandHandler languageCommandHandler;
    private final List<CommandHandler> handlers;

    @PostConstruct
    public void initializeHandlers() {
        for (CommandHandler handler : handlers) {
            String command = handler.getCommand();
            if (!command.equals("__UNKNOWN__") && !command.equals("__BEACH_NAME__")) {
                commandHandlers.put(command, handler);
                logger.info("Registered command handler: {}", command);
            }
        }
        
        // Register /lang as an alias for /language command
        if (commandHandlers.containsKey(BotConst.COMMAND_LANGUAGE)) {
            commandHandlers.put(BotConst.COMMAND_LANG, languageCommandHandler);
            logger.info("Registered command alias: {} -> {}", BotConst.COMMAND_LANG, BotConst.COMMAND_LANGUAGE);
        }
        
        logger.info("Command dispatcher initialized with {} handlers", commandHandlers.size());
    }

    public void dispatch(Update update, TelegramLongPollingBot bot) {
        try {
            // Handle callback queries (inline keyboard button clicks)
            if (update.hasCallbackQuery()) {
                String callbackData = update.getCallbackQuery().getData();
                logger.debug("Handling callback query: {}", callbackData);
                
                // Route callbacks to appropriate handlers
                if (callbackData.startsWith("lang_")) {
                    languageCommandHandler.handle(update, bot);
                } else {
                    logger.warn("Unknown callback query: {}", callbackData);
                }
                return;
            }
            
            // Handle text messages
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                String command = extractCommand(messageText);
                
                CommandHandler handler;
                
                if (messageText.startsWith("/")) {
                    // Handle bot commands
                    handler = commandHandlers.getOrDefault(command, unknownCommandHandler);
                    logger.debug("Handling command: {}", command);
                } else {
                    // Handle beach names (regular text)
                    handler = beachNameHandler;
                    logger.debug("Handling beach name: {}", messageText);
                }
                
                handler.handle(update, bot);
                logger.debug("Handled message with {}", handler.getClass().getSimpleName());
            }
        } catch (Exception e) {
            String identifier = update.hasMessage() ? update.getMessage().getText() : 
                               update.hasCallbackQuery() ? update.getCallbackQuery().getData() : "unknown";
            logger.error("Error handling update '{}': {}", identifier, e.getMessage(), e);
        }
    }

    private String extractCommand(String messageText) {
        // Extract command from message (e.g., "/start" or "/start@botname")
        if (messageText.startsWith("/")) {
            String[] parts = messageText.split(" ");
            String command = parts[0];
            // Remove bot username if present
            if (command.contains("@")) {
                command = command.substring(0, command.indexOf("@"));
            }
            return command;
        }
        return messageText;
    }
}
