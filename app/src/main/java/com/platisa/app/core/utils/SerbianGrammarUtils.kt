package com.platisa.app.core.utils

/**
 * Utility for handling Serbian grammar nuances.
 */
object SerbianGrammarUtils {

    /**
     * Converts a name to its Vocative (Vokativ) form for greetings.
     * 
     * Rules implemented:
     * 1. Male names ending in consonant: Add "-e" (e.g., Srđan -> Srđane)
     *    - Exception: "Petar" -> "Petre" (nepostojano a), "Aleksandar" -> "Aleksandre"
     * 2. Names ending in -o or -e: Remain unchanged (e.g., Marko -> Marko, Đorđe -> Đorđe)
     * 3. Female names ending in -a: Remain unchanged (e.g., Ana -> Ana, Marija -> Marija)
     *    - Exception: 3+ syllable names ending in -ica change to -ice (e.g., Milica -> Milice, Zorica -> Zorice)
     */
    fun toVocative(name: String): String {
        if (name.isBlank()) return name
        
        val trimmed = name.trim()
        val lower = trimmed.lowercase() // Use lowercase for easier checking
        
        // 2. Names ending in -o or -e remain unchanged
        if (lower.endsWith("o") || lower.endsWith("e") || lower.endsWith("i") || lower.endsWith("u")) {
            return trimmed
        }
        
        // 3. Names ending in -a
        if (lower.endsWith("a")) {
            // Exception: Trosložna imena na -ica (3+ syllables approx length check)
            // "Mica" (4 chars) -> Mico usually, but user explicit rule: "Trosložna imena na -ica: Menjaju nastavak u -ice"
            // "Milica" (6 chars), "Zorica" (6 chars), "Danica" (6 chars), "Verica" (6 chars)
            // Let's use length > 4 as a heuristic for 3+ syllables (Mica is 2 syllables)
            if (lower.endsWith("ica") && trimmed.length > 4) {
               return trimmed.dropLast(1) + "e"
            }
            return trimmed
        }
        
        // 1. Male names ending in consonant
        // Check for specific "nepostojano a" (fleeting a) exceptions
        if (lower.endsWith("tar") || lower.endsWith("dar")) {
             // Petar -> Petre, Aleksandar -> Aleksandre
             // Check if preceding char is consonant? usually yes.
             // Heuristic: drop 'a' and add 'e'
             // Handle: Lazar -> Lazare (exception to the exception!)
             if (lower == "lazar") {
                 return trimmed + "e"
             }
             
             // Extract the part before "ar"
             val prefix = trimmed.dropLast(2) // e.g. "Pet" from "Petar"
             return prefix + "re"
        }
        
        // Default rule for consonants: add "e"
        // Includes: Srdjan -> Srdjane, Ivan -> Ivane
        return trimmed + "e"
    }
}
