package com.example.platisa.core.domain.parser

import org.junit.Test
import org.junit.Assert.*

class MerchantCleaningTest {

    @Test
    fun testCleanMerchantName() {
        val input = "Telekom Srbija a.d. Takovska 2"
        val expected = "Telekom Srbija a.d."
        
        val cleaned = cleanMerchantName(input)
        assertEquals(expected, cleaned)
    }
    
    private fun cleanMerchantName(line: String): String {
        var cleaned = line
        
        // The exact regex I used
        val suffixRegex = Regex("\\b(d\\.?o\\.?o\\.?|a\\.?d\\.?|j\\.?p\\.?|j\\.?k\\.?p\\.?)(?=\\W|$)", RegexOption.IGNORE_CASE)
        val match = suffixRegex.find(cleaned)
        if (match != null) {
            cleaned = cleaned.substring(0, match.range.last + 1)
        }
        
        return cleaned.trim()
    }
}
