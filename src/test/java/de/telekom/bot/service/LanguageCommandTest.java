package de.telekom.bot.service;

import de.telekom.bot.util.BotConst;

/**
 * Manual test for language command functionality
 */
public class LanguageCommandTest {
    
    public static void main(String[] args) {
        System.out.println("🌍 Testing Language Command Functionality");
        System.out.println("=" + "=".repeat(50));
        
        // Test language constants
        System.out.println("\n📋 Language Constants:");
        System.out.println("English: " + BotConst.LANG_ENGLISH + " " + BotConst.FLAG_ENGLISH);
        System.out.println("Spanish: " + BotConst.LANG_SPANISH + " " + BotConst.FLAG_SPANISH);
        
        // Test UserLanguageService functionality
        UserLanguageService userLanguageService = new UserLanguageService();
        
        System.out.println("\n🔧 Testing UserLanguageService:");
        
        // Test default language
        Long testChatId = 123456L;
        String defaultLang = userLanguageService.getUserLanguage(testChatId);
        System.out.println("Default language for new user: " + defaultLang);
        
        // Test setting different languages
        System.out.println("\n📝 Setting languages:");
        userLanguageService.setUserLanguage(testChatId, BotConst.LANG_ENGLISH);
        System.out.println("Set English: " + userLanguageService.getUserLanguage(testChatId));
        
        userLanguageService.setUserLanguage(testChatId, BotConst.LANG_SPANISH);
        System.out.println("Set Spanish: " + userLanguageService.getUserLanguage(testChatId));
        
        // Test invalid language
        System.out.println("\n⚠️ Testing invalid language:");
        userLanguageService.setUserLanguage(testChatId, "fr");
        System.out.println("After invalid 'fr': " + userLanguageService.getUserLanguage(testChatId));
        
        // Test display names
        System.out.println("\n🎨 Display names:");
        System.out.println("English: " + userLanguageService.getLanguageDisplayName(BotConst.LANG_ENGLISH));
        System.out.println("Spanish: " + userLanguageService.getLanguageDisplayName(BotConst.LANG_SPANISH));
        
        // Test multiple users
        System.out.println("\n👥 Testing multiple users:");
        userLanguageService.setUserLanguage(111L, BotConst.LANG_ENGLISH);
        userLanguageService.setUserLanguage(222L, BotConst.LANG_SPANISH);
        userLanguageService.setUserLanguage(333L, BotConst.LANG_ENGLISH);
        
        System.out.println("Total users: " + userLanguageService.getUserCount());
        System.out.println("Language stats: " + userLanguageService.getLanguageStats());
        
        // Test commands
        System.out.println("\n🔧 Command constants:");
        System.out.println("Language command: " + BotConst.COMMAND_LANGUAGE);
        System.out.println("Lang command: " + BotConst.COMMAND_LANG);
        
        System.out.println("\n✅ All tests completed!");
    }
}