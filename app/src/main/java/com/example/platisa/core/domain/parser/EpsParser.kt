package com.example.platisa.core.domain.parser

import com.example.platisa.core.domain.model.EpsData

/**
 * Wrapper for EpsParser - delegates to data layer parser
 */
object EpsParser {
    fun parse(text: String): EpsData {
        return com.example.platisa.core.data.parser.EpsParser.parse(text)
    }
}
