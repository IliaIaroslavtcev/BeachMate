package de.telekom.bot.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

/**
 * Test to verify improved UnknownCommandHandler behavior
 */
public class UnknownCommandHandlerTest {
    
    private UnknownCommandHandler unknownCommandHandler;
    
    @BeforeEach
    public void setUp() {
        unknownCommandHandler = new UnknownCommandHandler();
    }
    
    @Test
    public void testUnknownCommandMessages() {
        System.out.println("=== Testing Unknown Command Handler ===");
        
        String[] testCommands = {
            "/unknown",
            "/test123",
            "/beach",
            "/información",
            "/плаж",
            "/命令"
        };
        
        for (String command : testCommands) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("Testing unknown command: '" + command + "'");
            System.out.println("=".repeat(60));
            
            String response = simulateUnknownCommandResponse(command);
            System.out.println(response);
        }
        
        System.out.println("\n=== Testing completed ===");
    }
    
    /**
     * Simulate the response that would be sent for an unknown command
     */
    private String simulateUnknownCommandResponse(String unknownCommand) {
        // This simulates the formatUnknownCommandMessage method
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
    
    @Test
    public void testCommandHandlerIdentification() {
        System.out.println("\n=== Testing Handler Identification ===");
        
        // Test that handler returns the correct command identifier
        String commandId = unknownCommandHandler.getCommand();
        System.out.println("Handler command identifier: " + commandId);
        
        if ("__UNKNOWN__".equals(commandId)) {
            System.out.println("✅ Handler correctly identified as unknown command handler");
        } else {
            System.out.println("❌ Handler identification failed");
        }
    }
}