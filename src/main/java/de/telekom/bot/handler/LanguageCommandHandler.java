package de.telekom.bot.handler;

import de.telekom.bot.service.UserLanguageService;
import de.telekom.bot.util.BotConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class LanguageCommandHandler implements CommandHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(LanguageCommandHandler.class);
    
    @Autowired
    private UserLanguageService userLanguageService;
    
    @Override
    public String getCommand() {
        return BotConst.COMMAND_LANGUAGE;
    }

    @Override
    public void handle(Update update, TelegramLongPollingBot bot) throws Exception {
        // Handle callback queries (when user clicks language button)
        if (update.hasCallbackQuery()) {
            handleLanguageSelection(update, bot);
            return;
        }
        
        // Handle regular /language command
        long chatId = update.getMessage().getChatId();
        String currentLanguage = userLanguageService.getUserLanguage(chatId);
        
        // Send language selection message with inline keyboard
        String messageText = getLanguageSelectionMessage(currentLanguage);
        SendMessage message = new SendMessage(String.valueOf(chatId), messageText);
        message.setParseMode("Markdown");
        message.setReplyMarkup(createLanguageKeyboard());
        
        // Reply to the original message to keep the response in the same topic
        if (update.getMessage() != null) {
            message.setReplyToMessageId(update.getMessage().getMessageId());
        }
        
        bot.execute(message);
    }
    
    private void handleLanguageSelection(Update update, TelegramLongPollingBot bot) throws Exception {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        
        logger.info("Handling language selection callback: {} from chat: {}", callbackData, chatId);
        
        if (callbackData.startsWith("lang_")) {
            String selectedLanguage = callbackData.substring(5); // Remove "lang_" prefix
            logger.info("User selected language: {}", selectedLanguage);
            
            if (userLanguageService.isValidLanguage(selectedLanguage)) {
                String previousLanguage = userLanguageService.getUserLanguage(chatId);
                userLanguageService.setUserLanguage(chatId, selectedLanguage);
                String newLanguage = userLanguageService.getUserLanguage(chatId);
                logger.info("Language changed from {} to {} for chat: {}", previousLanguage, newLanguage, chatId);
                
                // Edit the original message to show confirmation
                String confirmationMessage = getLanguageChangedMessage(selectedLanguage);
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(String.valueOf(chatId));
                editMessage.setMessageId(messageId);
                editMessage.setText(confirmationMessage);
                editMessage.setParseMode("Markdown");
                
                bot.execute(editMessage);
                
                // Answer the callback query to stop the loading indicator
                org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery answerCallback = 
                    new org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery();
                answerCallback.setCallbackQueryId(update.getCallbackQuery().getId());
                answerCallback.setText("Language updated! / ¡Idioma actualizado!");
                answerCallback.setShowAlert(false);
                
                bot.execute(answerCallback);
            }
        }
    }
    
    private String getLanguageSelectionMessage(String currentLanguage) {
        String currentLangDisplay = userLanguageService.getLanguageDisplayName(currentLanguage);
        
        return "🌍 *Language Selection / Selección de Idioma*\\n\\n" +
               "📢 **Current language:** *" + currentLangDisplay + "*\\n" +
               "📢 **Idioma actual:** *" + currentLangDisplay + "*\\n\\n" +
               "👆 **Please select your preferred language:**\\n" +
               "👆 **Por favor seleccione su idioma preferido:**";
    }
    
    private String getLanguageChangedMessage(String newLanguage) {
        switch (newLanguage) {
            case BotConst.LANG_ENGLISH:
                return "✅ *Language changed successfully!*\\n\\n" +
                       "🇬🇧 Your language has been set to *English*.\\n" +
                       "Now all bot responses will be in English.\\n\\n" +
                       "💡 You can change language anytime using /language command.";
                       
            case BotConst.LANG_SPANISH:
            default:
                return "✅ *¡Idioma cambiado con éxito!*\\n\\n" +
                       "🇪🇸 Tu idioma ha sido establecido a *Español*.\\n" +
                       "Ahora todas las respuestas del bot estarán en español.\\n\\n" +
                       "💡 Puedes cambiar el idioma en cualquier momento usando el comando /language.";
        }
    }
    
    private InlineKeyboardMarkup createLanguageKeyboard() {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        Map<String, String> languages = userLanguageService.getAllLanguages();
        
        for (Map.Entry<String, String> entry : languages.entrySet()) {
            String langCode = entry.getKey();
            String langDisplay = entry.getValue();
            
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(langDisplay);
            button.setCallbackData("lang_" + langCode);
            
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            keyboard.add(row);
        }
        
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
}