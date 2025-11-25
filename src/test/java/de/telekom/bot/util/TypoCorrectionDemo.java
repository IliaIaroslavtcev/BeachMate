package de.telekom.bot.util;

/**
 * Console demo to test typo correction functionality
 */
public class TypoCorrectionDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Typo Correction Demo ===");
        
        String[] testCases = {
            "benidrm",      // Missing 'o'
            "benidrom",     // Swapped 'o' and 'm'
            "benedorm",     // Wrong vowel
            "binidorm",     // Wrong consonant
            "alicant",      // Missing 'e'
            "valecia",      // Wrong consonant
            "marbela",      // Missing 'l'
            "barcelon",     // Missing 'a'
            "malga",        // Missing letters
            "benidorm",     // Correct (should show no correction)
            "valencia",     // Correct (should show no correction)
            "xyz",          // Too different (should show no correction)
            "BENIDRM",      // Case test
            "BenIdRom"      // Mixed case test
        };
        
        for (String testCase : testCases) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("Testing: '" + testCase + "'");
            System.out.println("=".repeat(50));
            
            TypoCorrection.TypoCorrectionSuggestion suggestion = TypoCorrection.findBestCorrection(testCase);
            
            if (suggestion != null) {
                System.out.println("✅ CORRECTION FOUND!");
                System.out.println("Original: " + suggestion.getOriginalInput());
                System.out.println("Suggested: " + suggestion.getSuggestedCorrection());
                System.out.println("Capitalized: " + suggestion.getCapitalizedSuggestion());
                System.out.println("Edit Distance: " + suggestion.getEditDistance());
                
                // Show what the bot message would look like
                System.out.println("\n--- Bot Message Preview ---");
                System.out.println("🤔 Hmm, location not found...");
                System.out.println("❓ You searched for: " + suggestion.getOriginalInput());
                System.out.println("💡 Did you mean: " + suggestion.getCapitalizedSuggestion() + "?");
                System.out.println("🔄 Try typing: " + suggestion.getSuggestedCorrection());
            } else {
                System.out.println("❌ No correction suggested");
                if (TypoCorrection.getAllKnownDestinations().contains(testCase.toLowerCase())) {
                    System.out.println("✅ (This is already correct!)");
                } else {
                    System.out.println("📝 (Too different from known destinations)");
                }
            }
        }
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("Known destinations:");
        System.out.println("=".repeat(50));
        TypoCorrection.getAllKnownDestinations().stream()
            .sorted()
            .forEach(dest -> System.out.println("• " + dest));
        
        System.out.println("\n=== Demo completed ===");
    }
}