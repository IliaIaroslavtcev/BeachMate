package de.telekom.bot.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for typo correction functionality
 */
public class TypoCorrectionTest {
    
    @Test
    public void testBenidormTypos() {
        // Test common Benidorm typos
        assertCorrection("benidrm", "benidorm");
        assertCorrection("benidrom", "benidorm");
        assertCorrection("benedorm", "benidorm");
        assertCorrection("benidom", "benidorm");
        assertCorrection("binidorm", "benidorm");
    }
    
    @Test
    public void testValenciaTypos() {
        assertCorrection("valecia", "valencia");
        assertCorrection("valensia", "valencia");
        assertCorrection("balencia", "valencia");
    }
    
    @Test
    public void testAlicanteTypos() {
        assertCorrection("alicant", "alicante");
        assertCorrection("aliecante", "alicante");
        assertCorrection("alikante", "alicante");
    }
    
    @Test
    public void testExactMatches() {
        // Exact matches should return null (no correction needed)
        assertNoCorrection("benidorm");
        assertNoCorrection("valencia");
        assertNoCorrection("alicante");
    }
    
    @Test
    public void testCaseInsensitive() {
        assertCorrection("BENIDRM", "benidorm");
        assertCorrection("BenIdRoM", "benidorm");
        assertNoCorrection("VALENCIA"); // Exact match, no correction
    }
    
    @Test
    public void testTooManyErrors() {
        // Should not suggest corrections for strings too different
        assertNoCorrection("xyz");
        assertNoCorrection("abcdefghijk");
        assertNoCorrection("12345");
    }
    
    @Test
    public void testEmptyAndNull() {
        assertNoCorrection("");
        assertNoCorrection(null);
        assertNoCorrection("   ");
    }
    
    @Test
    public void testDistanceCalculation() {
        TypoCorrection.TypoCorrectionSuggestion suggestion = TypoCorrection.findBestCorrection("benidrm");
        assertNotNull(suggestion);
        assertEquals("benidorm", suggestion.getSuggestedCorrection());
        assertEquals(1, suggestion.getEditDistance()); // Only 1 character missing ('o')
        
        // Test another case with 2 character difference
        suggestion = TypoCorrection.findBestCorrection("benedom");
        assertNotNull(suggestion);
        assertEquals("benidorm", suggestion.getSuggestedCorrection());
        assertTrue(suggestion.getEditDistance() <= 2); // Should be 2 or less
    }
    
    @Test
    public void testCapitalization() {
        TypoCorrection.TypoCorrectionSuggestion suggestion = TypoCorrection.findBestCorrection("benidrm");
        assertNotNull(suggestion);
        assertEquals("benidorm", suggestion.getSuggestedCorrection());
        assertEquals("Benidorm", suggestion.getCapitalizedSuggestion());
    }
    
    private void assertCorrection(String input, String expected) {
        TypoCorrection.TypoCorrectionSuggestion suggestion = TypoCorrection.findBestCorrection(input);
        assertNotNull(suggestion, "Should find a correction for: " + input);
        assertEquals(expected, suggestion.getSuggestedCorrection(), 
                    "Wrong correction for: " + input);
    }
    
    private void assertNoCorrection(String input) {
        TypoCorrection.TypoCorrectionSuggestion suggestion = TypoCorrection.findBestCorrection(input);
        assertNull(suggestion, "Should not find correction for: " + input);
    }
}