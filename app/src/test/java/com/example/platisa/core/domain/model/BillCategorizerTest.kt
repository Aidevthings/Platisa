package com.platisa.app.core.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests za BillCategorizer - kategorizacija računa po trgovcu.
 */
class BillCategorizerTest {

    // ============================================
    // ELECTRICITY CATEGORY TESTS
    // ============================================

    @Test
    fun `categorize EPS in latin`() {
        val result = BillCategorizer.categorize("EPS SNABDEVANJE D.O.O.")
        assertEquals(BillCategory.ELECTRICITY, result)
    }

    @Test
    fun `categorize EPS in cyrillic`() {
        val result = BillCategorizer.categorize("ЕПС СНАБДЕВАЊЕ Д.О.О.")
        assertEquals(BillCategory.ELECTRICITY, result)
    }

    @Test
    fun `categorize Elektrodistribucija`() {
        val result = BillCategorizer.categorize("ELEKTRODISTRIBUCIJA BEOGRAD")
        assertEquals(BillCategory.ELECTRICITY, result)
    }

    @Test
    fun `categorize with elektr keyword`() {
        val result = BillCategorizer.categorize("Električna energija")
        assertEquals(BillCategory.ELECTRICITY, result)
    }

    @Test
    fun `categorize struja`() {
        val result = BillCategorizer.categorize("Račun za struju")
        assertEquals(BillCategory.ELECTRICITY, result)
    }

    // ============================================
    // WATER CATEGORY TESTS
    // ============================================

    @Test
    fun `categorize Vodovod`() {
        val result = BillCategorizer.categorize("JKP VODOVOD I KANALIZACIJA")
        assertEquals(BillCategory.WATER, result)
    }

    @Test
    fun `categorize Vodovod cyrillic`() {
        val result = BillCategorizer.categorize("ЈКП ВОДОВОД И КАНАЛИЗАЦИЈА")
        assertEquals(BillCategory.WATER, result)
    }

    @Test
    fun `categorize Voda`() {
        val result = BillCategorizer.categorize("Račun za vodu")
        assertEquals(BillCategory.WATER, result)
    }

    // ============================================
    // TELECOM CATEGORY TESTS
    // ============================================

    @Test
    fun `categorize Telekom`() {
        val result = BillCategorizer.categorize("TELEKOM SRBIJA A.D.")
        assertEquals(BillCategory.TELECOM, result)
    }

    @Test
    fun `categorize Telekom cyrillic`() {
        val result = BillCategorizer.categorize("ТЕЛЕКОМ СРБИЈА А.Д.")
        assertEquals(BillCategory.TELECOM, result)
    }

    @Test
    fun `categorize MTS`() {
        val result = BillCategorizer.categorize("MTS mobilni")
        assertEquals(BillCategory.TELECOM, result)
    }

    @Test
    fun `categorize MTS cyrillic`() {
        val result = BillCategorizer.categorize("МТС мобилни")
        assertEquals(BillCategory.TELECOM, result)
    }

    @Test
    fun `categorize A1`() {
        val result = BillCategorizer.categorize("A1 SRBIJA")
        assertEquals(BillCategory.TELECOM, result)
    }

    @Test
    fun `categorize Yettel`() {
        val result = BillCategorizer.categorize("YETTEL D.O.O.")
        assertEquals(BillCategory.TELECOM, result)
    }

    @Test
    fun `categorize VIP mobile`() {
        val result = BillCategorizer.categorize("VIP MOBILE")
        assertEquals(BillCategory.TELECOM, result)
    }

    // ============================================
    // GAS CATEGORY TESTS
    // ============================================

    @Test
    fun `categorize Gas`() {
        val result = BillCategorizer.categorize("SRBIJAGAS A.D.")
        assertEquals(BillCategory.GAS, result)
    }

    @Test
    fun `categorize Gas cyrillic`() {
        val result = BillCategorizer.categorize("СРБИЈАГАС А.Д.")
        assertEquals(BillCategory.GAS, result)
    }

    @Test
    fun `categorize Plin`() {
        val result = BillCategorizer.categorize("Distribucija plina")
        assertEquals(BillCategory.GAS, result)
    }

    // ============================================
    // UTILITIES CATEGORY TESTS
    // ============================================

    @Test
    fun `categorize Infostan`() {
        val result = BillCategorizer.categorize("JKP INFOSTAN TEHNOLOGIJE")
        assertEquals(BillCategory.UTILITIES, result)
    }

    @Test
    fun `categorize Infostan cyrillic`() {
        val result = BillCategorizer.categorize("ЈКП ИНФОСТАН ТЕХНОЛОГИЈЕ")
        assertEquals(BillCategory.UTILITIES, result)
    }

    @Test
    fun `categorize Komunalne usluge`() {
        val result = BillCategorizer.categorize("Komunalne usluge")
        assertEquals(BillCategory.UTILITIES, result)
    }

    // ============================================
    // PHARMACY CATEGORY TESTS
    // ============================================

    @Test
    fun `categorize Apoteka`() {
        val result = BillCategorizer.categorize("APOTEKA BENU")
        assertEquals(BillCategory.PHARMACY, result)
    }

    @Test
    fun `categorize Apoteka cyrillic`() {
        val result = BillCategorizer.categorize("АПОТЕКА БЕНУ")
        assertEquals(BillCategory.PHARMACY, result)
    }

    @Test
    fun `categorize DM Drogerie`() {
        val result = BillCategorizer.categorize("DM DROGERIE MARKT")
        assertEquals(BillCategory.PHARMACY, result)
    }

    @Test
    fun `categorize Lilly`() {
        val result = BillCategorizer.categorize("LILLY DROGERIJA")
        assertEquals(BillCategory.PHARMACY, result)
    }

    @Test
    fun `categorize DrMax`() {
        val result = BillCategorizer.categorize("DR.MAX APOTEKA")
        assertEquals(BillCategory.PHARMACY, result)
    }

    // ============================================
    // RESTAURANT CATEGORY TESTS
    // ============================================

    @Test
    fun `categorize Restoran`() {
        val result = BillCategorizer.categorize("RESTORAN MADERA")
        assertEquals(BillCategory.RESTAURANT, result)
    }

    @Test
    fun `categorize Restoran cyrillic`() {
        val result = BillCategorizer.categorize("РЕСТОРАН МАДЕРА")
        assertEquals(BillCategory.RESTAURANT, result)
    }

    @Test
    fun `categorize Kafic`() {
        val result = BillCategorizer.categorize("CAFFE BAR CENTRAL")
        assertEquals(BillCategory.RESTAURANT, result)
    }

    @Test
    fun `categorize Pizza`() {
        val result = BillCategorizer.categorize("PIZZA HUT")
        assertEquals(BillCategory.RESTAURANT, result)
    }

    @Test
    fun `categorize Pekara`() {
        val result = BillCategorizer.categorize("PEKARA TOMA")
        assertEquals(BillCategory.RESTAURANT, result)
    }

    @Test
    fun `categorize McDonalds`() {
        val result = BillCategorizer.categorize("McDonald's Srbija")
        assertEquals(BillCategory.RESTAURANT, result)
    }

    // ============================================
    // GROCERY CATEGORY TESTS
    // ============================================

    @Test
    fun `categorize Maxi`() {
        val result = BillCategorizer.categorize("MAXI SUPERMARKET")
        assertEquals(BillCategory.GROCERY, result)
    }

    @Test
    fun `categorize Maxi cyrillic`() {
        val result = BillCategorizer.categorize("МАКСИ СУПЕРМАРКЕТ")
        assertEquals(BillCategory.GROCERY, result)
    }

    @Test
    fun `categorize Idea`() {
        val result = BillCategorizer.categorize("IDEA D.O.O.")
        assertEquals(BillCategory.GROCERY, result)
    }

    @Test
    fun `categorize Lidl`() {
        val result = BillCategorizer.categorize("LIDL SRBIJA")
        assertEquals(BillCategory.GROCERY, result)
    }

    @Test
    fun `categorize Roda`() {
        val result = BillCategorizer.categorize("RODA MEGAMARKET")
        assertEquals(BillCategory.GROCERY, result)
    }

    @Test
    fun `categorize Univerexport`() {
        val result = BillCategorizer.categorize("UNIVEREXPORT D.O.O.")
        assertEquals(BillCategory.GROCERY, result)
    }

    @Test
    fun `categorize DIS`() {
        val result = BillCategorizer.categorize("DIS MARKET")
        assertEquals(BillCategory.GROCERY, result)
    }

    @Test
    fun `categorize generic market`() {
        val result = BillCategorizer.categorize("MINI MARKET PERO")
        assertEquals(BillCategory.GROCERY, result)
    }

    @Test
    fun `categorize STR`() {
        val result = BillCategorizer.categorize("S.T.R. PERO")
        assertEquals(BillCategory.GROCERY, result)
    }

    // ============================================
    // OTHER CATEGORY TESTS
    // ============================================

    @Test
    fun `categorize unknown merchant as OTHER`() {
        val result = BillCategorizer.categorize("RANDOM COMPANY D.O.O.")
        assertEquals(BillCategory.OTHER, result)
    }

    @Test
    fun `categorize empty string as OTHER`() {
        val result = BillCategorizer.categorize("")
        assertEquals(BillCategory.OTHER, result)
    }

    @Test
    fun `categorize numbers only as OTHER`() {
        val result = BillCategorizer.categorize("123456789")
        assertEquals(BillCategory.OTHER, result)
    }

    // ============================================
    // CASE INSENSITIVITY TESTS
    // ============================================

    @Test
    fun `categorize is case insensitive for latin`() {
        assertEquals(BillCategory.ELECTRICITY, BillCategorizer.categorize("eps snabdevanje"))
        assertEquals(BillCategory.ELECTRICITY, BillCategorizer.categorize("EPS SNABDEVANJE"))
        assertEquals(BillCategory.ELECTRICITY, BillCategorizer.categorize("Eps Snabdevanje"))
    }

    @Test
    fun `categorize is case insensitive for cyrillic`() {
        assertEquals(BillCategory.ELECTRICITY, BillCategorizer.categorize("епс снабдевање"))
        assertEquals(BillCategory.ELECTRICITY, BillCategorizer.categorize("ЕПС СНАБДЕВАЊЕ"))
    }

    // ============================================
    // SPECIAL CHARACTERS TESTS
    // ============================================

    @Test
    fun `categorize handles special Serbian characters`() {
        assertEquals(BillCategory.ELECTRICITY, BillCategorizer.categorize("ELEKTRIČNA ENERGIJA"))
        assertEquals(BillCategory.WATER, BillCategorizer.categorize("VODOVODČŠŽĆĐ"))
    }

    // ============================================
    // PRIORITY TESTS - Pharmacy before Grocery
    // ============================================

    @Test
    fun `pharmacy takes priority over grocery for DM`() {
        // DM is a drugstore, not a grocery store
        val result = BillCategorizer.categorize("DM DROGERIE")
        assertEquals(BillCategory.PHARMACY, result)
    }

    // ============================================
    // BILL CATEGORY ENUM TESTS
    // ============================================

    @Test
    fun `BillCategory displayName is in Serbian`() {
        assertEquals("Struja", BillCategory.ELECTRICITY.displayName)
        assertEquals("Voda", BillCategory.WATER.displayName)
        assertEquals("Telefon", BillCategory.TELECOM.displayName)
        assertEquals("Gas", BillCategory.GAS.displayName)
        assertEquals("Komunalije", BillCategory.UTILITIES.displayName)
        assertEquals("Apoteka", BillCategory.PHARMACY.displayName)
        assertEquals("Restorani", BillCategory.RESTAURANT.displayName)
        assertEquals("Namirnice", BillCategory.GROCERY.displayName)
        assertEquals("Ostalo", BillCategory.OTHER.displayName)
    }
}

