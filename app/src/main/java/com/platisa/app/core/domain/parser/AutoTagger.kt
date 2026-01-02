package com.platisa.app.core.domain.parser

object AutoTagger {

    private val rules = mapOf(
        "Groceries" to listOf("MAXI", "LIDL", "IDEA", "RODA", "UNIVEREXPORT", "DIS"),
        "Bills" to listOf("EPS", "TELEKOM", "SBB", "YETTEL", "A1", "INFOSTAN"),
        "Fuel" to listOf("NIS", "OMV", "MOL", "EKO", "LUKOIL"),
        "Dining" to listOf("RESTORAN", "CAFFE", "KAFANA", "MCDONALDS", "KFC")
    )

    fun suggestSection(text: String): String? {
        val upperText = text.uppercase()
        for ((section, keywords) in rules) {
            if (keywords.any { upperText.contains(it) }) {
                return section
            }
        }
        return null
    }

    fun suggestTags(text: String): List<String> {
        val tags = mutableListOf<String>()
        val upperText = text.uppercase()
        
        // Example tag rules
        if (upperText.contains("HLEB") || upperText.contains("MLEKO")) tags.add("Essentials")
        if (upperText.contains("PIVO") || upperText.contains("VINO")) tags.add("Alcohol")
        if (upperText.contains("GORIVO") || upperText.contains("BMB")) tags.add("Car")

        return tags
    }
}

