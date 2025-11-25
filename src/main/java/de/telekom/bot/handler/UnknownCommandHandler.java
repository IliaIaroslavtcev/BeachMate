package de.telekom.bot.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class UnknownCommandHandler implements CommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(UnknownCommandHandler.class);

    @Override
    public String getCommand() {
        // Special key for unknown commands
        return "__UNKNOWN__";
    }

    @Override
    public void handle(Update update, TelegramLongPollingBot bot) throws Exception {
        long chatId = update.getMessage().getChatId();
        String unknownCommand = update.getMessage().getText();

        logger.info("Unknown command received: '{}' from chat: {}", unknownCommand, chatId);

        String helpfulMessage = formatUnknownCommandMessage(unknownCommand);

        SendMessage message = new SendMessage(String.valueOf(chatId), helpfulMessage);
        message.setParseMode("Markdown");
        
        // Reply to the original message to keep the response in the same topic
        if (update.getMessage() != null) {
            message.setReplyToMessageId(update.getMessage().getMessageId());
        }

        bot.execute(message);
    }

    /**
     * Format a helpful message for unknown commands
     */
    private String formatUnknownCommandMessage(String unknownCommand) {
        return "🤔 *Command not recognized* \n\n" +
                "❓ **You typed:** `" + unknownCommand + "`\n\n" +
                "💡 **Here's what I can help with:**\n\n" +
                "🔹 **Available commands:**\n" +
                "• `/start` - Get started & introduction\n" +
                "• `/help` - Detailed help & examples\n\n" +
                "🏖️ **Looking for beach information?**\n" +
                "Just type the beach name directly (no / needed!)\n\n" +
                "📝 **Examples:**\n" +
                "• `benidorm` - Get Benidorm beach info\n" +
                "• `valencia` - Get Valencia beach info\n" +
                "• `marbella` - Get Marbella beach info\n\n" +
                "💬 *Try typing a beach name or use /help for more examples!*";
    }
}

