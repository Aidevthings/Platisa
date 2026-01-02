package com.platisa.app.core.domain.parser

import com.platisa.app.core.domain.model.EpsData

/**
 * Wrapper for EpsParser - delegates to data layer parser
 */
object EpsParser {
    fun parse(text: String): EpsData {
        return com.platisa.app.core.data.parser.EpsParser.parse(text)
    }
}

