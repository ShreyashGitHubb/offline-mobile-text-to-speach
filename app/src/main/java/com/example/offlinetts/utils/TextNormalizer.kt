package com.example.offlinetts.utils

import java.util.regex.Pattern

object TextNormalizer {

    fun normalize(text: String): String {
        var normalized = text

        // 1. Numbers to words (Simple approximation)
        // A full implementation would need a library like ICU4J or custom rule set
        normalized = normalized.replace(Regex("\\b(\\d+)\\b")) { matchResult ->
            // Placeholder: Just keeping digits for now or could implement a basic number converter
            // For now, let's leave digits as Espeak usually handles them reasonably well
            matchResult.value 
        }

        // 2. Abbreviations
        val abbreviations = mapOf(
            "Mr." to "Mister",
            "Mrs." to "Missus",
            "Dr." to "Doctor",
            "St." to "Street",
            "etc." to "et cetera",
            "e.g." to "for example",
            "i.e." to "that is"
        )
        
        abbreviations.forEach { (abbr, full) ->
            normalized = normalized.replace(abbr, full, ignoreCase = true)
        }

        // 3. Currency (Basic)
        normalized = normalized.replace(Regex("\\$(\\d+)")) { "\${it.groupValues[1]} dollars" }

        // 4. Time (Basic 12:00)
        normalized = normalized.replace(Regex("(\\d{1,2}):(\\d{2})")) { 
            "\${it.groupValues[1]} \${it.groupValues[2]}" // Simplified
        }

        // 5. Cleanup whitespace
        normalized = normalized.replace(Regex("\\s+"), " ").trim()

        return normalized
    }

    fun splitIntoSentences(text: String): List<String> {
        // Basic split by punctuation followed by space
        return text.split(Regex("(?<=[.!?])\\s+"))
            .filter { it.isNotBlank() }
    }
}
