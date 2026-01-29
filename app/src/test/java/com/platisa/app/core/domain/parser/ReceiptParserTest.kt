package com.platisa.app.core.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.math.BigDecimal

class ReceiptParserTest {

    @Test
    fun `parse extracts merchant name correctly`() {
        val receiptText = """
            104863 LIDL SRBIJA KD
            ODRANSKA BB
            NOVI SAD
            PIB: 104863212
            -------------------
            ARTIKLI
            ============
            TOTAL: 1200 RSD
        """.trimIndent()
        
        val result = ReceiptParser.parse(receiptText)
        assertNotNull(result.merchantName)
    }

    @Test
    fun `parse extracts total amount correctly`() {
        val receiptText = """
            SHOP ABC
            Item 1 100
            Item 2 200
            ЗА УПЛАТУ: 300.00
            Hvala na poseti
        """.trimIndent()
        
        val result = ReceiptParser.parse(receiptText)
        assertEquals(BigDecimal("300.00"), result.totalAmount)
    }

    @Test
    fun `parse extracts items correctly`() {
        val receiptText = """
            MERCHANT NAME
            Mleko 1L
            120,00 2 240,00 A
            Hleb
            60,00 1 60,00 Ђ
            UKUPNO: 300,00
        """.trimIndent()

        val result = ReceiptParser.parse(receiptText)
        
        assertEquals(2, result.items.size)
        // Check simple existence
        assertEquals(BigDecimal("240.00"), result.items[0].total)
    }

    @Test
    fun `parse extracts Infostan Cyrillic address correctly`() {
        // REPRODUCTION CASE from User Screenshot
        val receiptText = """
            ЈКП ИНФОСТАН ТЕХНОЛОГИЈЕ
            Данијелова 33, 11010 Београд
            
            ADRESA OBJEKTA
            НИНКОВИЋ НИКОЛА Општина: НОВИ БЕОГРАД Насеље: БГ*Н.БЕОГРАД
            СУРЧИНСКИ ПУТ 16 СТ. 10
            
            UKUPNO: 12.000,00
        """.trimIndent()

        val result = ReceiptParser.parse(receiptText)

        // Expected Logic:
        // 1. Parser finds "Општина" (Cyrillic) in line "НИНКОВИЋ..."
        // 2. Parser extracts Opstina: "НОВИ БЕОГРАД" (stripping Naselje)
        // 3. Parser CAPTURES NEXT LINE: "СУРЧИНСКИ ПУТ 16 СТ. 10"
        // 4. Returns formatted string.
        
        println("Result Address: ${result.merchantAddress}")
        
        // Assert address contains the street
        assertNotNull(result.merchantAddress)
        assert(result.merchantAddress!!.contains("СУРЧИНСКИ ПУТ 16"))
        assert(result.merchantAddress!!.contains("НОВИ БЕОГРАД"))
    }
}
