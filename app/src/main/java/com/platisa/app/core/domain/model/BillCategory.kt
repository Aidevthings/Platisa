package com.platisa.app.core.domain.model

import com.platisa.app.ui.theme.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class BillCategory(val icon: ImageVector, val color: Color, val displayName: String) {
    ELECTRICITY(Icons.Default.Lightbulb, ElectricYellow, "Struja"),
    WATER(Icons.Default.WaterDrop, DeepCyan, "Voda"),
    TELECOM(Icons.Default.Phone, NeonPurple, "Telefon"),
    GAS(Icons.Default.LocalFireDepartment, DeepOrange, "Gas"),
    GROCERY(Icons.Default.ShoppingCart, Color(0xFF4CAF50), "Namirnice"),
    PHARMACY(Icons.Default.LocalHospital, Color(0xFFE91E63), "Apoteka"),
    RESTAURANT(Icons.Default.Restaurant, Color(0xFFFF9800), "Restorani"), // Orange
    UTILITIES(Icons.Default.LocationCity, Color(0xFF607D8B), "Komunalije"), // BlueGrey
    OTHER(Icons.Default.Receipt, MatrixGreen, "Ostalo")
}

object BillCategorizer {
    
    fun categorize(merchantName: String): BillCategory {
        val normalized = merchantName.uppercase()
        
        // Electricity keywords
        if (normalized.contains("EPS") || 
            normalized.contains("ELEKTR") || 
            normalized.contains("STRUJ") ||
            normalized.contains("ЕПС") ||
            normalized.contains("ЕЛЕКТР") ||
            normalized.contains("СТРУЈ") ||
            normalized.contains("ELECTRIC") ||
            normalized.contains("POWER")) {
            return BillCategory.ELECTRICITY
        }
        
        // Water keywords
        if (normalized.contains("VODOVOD") ||
            normalized.contains("ВОДОВОД") ||
            normalized.contains("WATER") ||
            normalized.contains("VODA") ||
            normalized.contains("ВОДА")) {
            return BillCategory.WATER
        }
        
        // Telecom keywords
        if (normalized.contains("TELEKOM") ||
            normalized.contains("ТЕЛЕКОМ") ||
            normalized.contains("MTS") ||
            normalized.contains("МТС") ||
            normalized.contains("A1") ||
            normalized.contains("А1") ||
            normalized.contains("YETTEL") ||
            normalized.contains("ЈЕТЕЛ") ||
            normalized.contains("VIP") ||
            normalized.contains("ВИП") ||
            normalized.contains("PHONE") ||
            normalized.contains("MOBILE") ||
            normalized.contains("TELEFONIJA") ||
            normalized.contains("ТЕЛЕФОНИЈА")) {
            return BillCategory.TELECOM
        }
        
        // Gas keywords
        if (normalized.contains("GAS") ||
            normalized.contains("ГАС") ||
            normalized.contains("PLIN") ||
            normalized.contains("ПЛИН") ||
            normalized.contains("SRBIJAGAS") ||
            normalized.contains("СРБИЈАГАС")) {
            return BillCategory.GAS
        }

        // Utilities (Infostan, etc.)
        if (normalized.contains("INFOSTAN") ||
            normalized.contains("ИНФОСТАН") ||
            normalized.contains("KOMUNAL") ||
            normalized.contains("КОМУНАЛ") ||
            normalized.contains("STAMBEN") ||
            normalized.contains("СТАМБЕН") ||
            normalized.contains("REZIJE") ||
            normalized.contains("REZID") ||
            normalized.contains("РЕЖИЈЕ")) {
            return BillCategory.UTILITIES
        }

        // Pharmacy keywords (CHECK BEFORE GROCERY)
        if (normalized.contains("APOTEKA") ||
            normalized.contains("АПОТЕКА") ||
            normalized.contains("ZDRAVSTVENA") ||
            normalized.contains("ЗДРАВСТВЕНА") ||
            normalized.contains("PHARMACY") ||
            normalized.contains("ФАРМАЦИЈА") ||
            normalized.contains("LILY") ||
            normalized.contains("LILLY") ||
            normalized.contains("ЛИЛИ") ||
            normalized.contains("DM ") || 
            normalized.contains("ДМ ") ||
            normalized.startsWith("DM ") ||
            normalized.startsWith("ДМ ") ||
            normalized == "DM" ||
            normalized == "ДМ" ||
            normalized.contains("DM-") ||
            normalized.contains("DROGERI") || // Catches Drogerija, Drogerije...
            normalized.contains("ДРОГЕРИ") ||
            normalized.contains("COSMETICS") ||
            normalized.contains("KOZMETIKA") ||
            normalized.contains("КОЗМЕТИКА") ||
            normalized.contains("PARFIMERIJA") ||
            normalized.contains("ПАРФИМЕРИЈА") ||
            normalized.contains("BENU") ||
            normalized.contains("БЕНУ") ||
            normalized.contains("DR.MAX") ||
            normalized.contains("ДР.МАX") ||
            normalized.contains("KONZILIJUM") ||
            normalized.contains("КОНЗИЛИЈУМ") ||
            normalized.contains("LABORATORIJ") ||
            normalized.contains("ЛАБОРАТОРИЈ") ||
            normalized.contains("ANALIZA") ||
            normalized.contains("АНАЛИЗА") ||
            normalized.contains("POLIKLINIKA") ||
            normalized.contains("ПОЛИКЛИНИКА") ||
            normalized.contains("KLINIKA") ||
            normalized.contains("КЛИНИКА") ||
            normalized.contains("LEKAR") ||
            normalized.contains("ЛЕКАР") ||
            normalized.contains("САНИТЕТ")) {
            return BillCategory.PHARMACY
        }

        // Restaurant keywords
        if (normalized.contains("RESTORAN") ||
            normalized.contains("РЕСТОРАН") ||
            normalized.contains("KAFE") ||
            normalized.contains("КАФЕ") ||
            normalized.contains("CAFFE") ||
            normalized.contains("BISTRO") ||
            normalized.contains("БИСТРО") ||
            normalized.contains("PIZZA") ||
            normalized.contains("ПИЦА") ||
            normalized.contains("BURGER") ||
            normalized.contains("БУРГЕР") ||
            normalized.contains("GRILL") ||
            normalized.contains("ГРИЛ") ||
            normalized.contains("ROŠTILJ") ||
            normalized.contains("РОШТИЉ") ||
            normalized.contains("PEKARA") ||
            normalized.contains("ПЕКАРА") ||
            normalized.contains("UGOSTITELJ") ||
            normalized.contains("УГОСТИТЕЉ") ||
            normalized.contains("HRANA I PIĆE") ||
            normalized.contains("HRANA I PICE") ||
            normalized.contains("ХРАНА И ПИЋЕ") ||
            normalized.contains("KAFANA") ||
            normalized.contains("КАФАНА") ||
            normalized.contains("SLASTICARNA") || 
            normalized.contains("POSLASTICARNICA") ||
            normalized.contains("ПОСЛАСТИЧАРНИЦА") ||
            normalized.contains("McDonald") ||
            normalized.contains("KFC") ||
            normalized.contains("WALTER")) {
            return BillCategory.RESTAURANT
        }

        // Grocery keywords
        if (normalized.contains("MAXI") ||
            normalized.contains("МАКСИ") ||
            normalized.contains("IDEA") ||
            normalized.contains("ИДЕА") ||
            normalized.contains("LIDL") ||
            normalized.contains("ЛИДЛ") ||
            normalized.contains("RODA") ||
            normalized.contains("РОДА") ||
            normalized.contains("UNIVEREXPORT") ||
            normalized.contains("УНИВЕРЕXПОРТ") ||
            normalized.contains("MERCATOR") ||
            normalized.contains("МЕРКАТОР") ||
            normalized.contains("AROMA") ||
            normalized.contains("АРОМА") ||
            normalized.contains("DIS") ||
            normalized.contains("ДИС") ||
            normalized.contains("SUPER VERO") ||
            normalized.contains("СУПЕР ВЕРО") ||
            normalized.contains("METRO") ||
            normalized.contains("МЕТРО") ||
            normalized.contains("PRODAVNICA") || 
            normalized.contains("ПРОДАВНИЦА") ||
            normalized.contains("MARKET") ||      
            normalized.contains("МАРКЕТ") ||
            normalized.contains("TRGOVINA") ||    
            normalized.contains("ТРГОВИНА") ||
            normalized.contains("TRGOVINU") ||
            normalized.contains("ТРГОВИНУ") ||
            normalized.contains("S.T.R.") ||      
            normalized.contains("С.Т.Р.") ||
            normalized.contains("STR ") ||
            normalized.contains("СТР ")) {
            return BillCategory.GROCERY
        }
        
        // Default
        return BillCategory.OTHER
    }
}

