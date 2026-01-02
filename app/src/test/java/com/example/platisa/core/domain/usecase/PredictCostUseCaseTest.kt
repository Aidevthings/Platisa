package com.example.platisa.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.platisa.core.domain.model.EpsMonthData
import java.math.BigDecimal

class PredictCostUseCaseTest {

    private val useCase = PredictCostUseCase()

    @Test
    fun `predict with insufficient data returns low confidence`() {
        val data = listOf(
            EpsMonthData("Jan", BigDecimal("350"), BigDecimal("120"), BigDecimal("5000"))
        )

        val result = useCase(data)

        assertEquals("Confidence check", 0.0, result.confidenceLevel, 0.1)
        assertTrue(result.alerts.any { it.message.contains("Nedovoljno podataka") })
    }

    @Test
    fun `predict with stable trend`() {
        val data = listOf(
            EpsMonthData("Jan", BigDecimal("350"), BigDecimal("120"), BigDecimal("5000")),
            EpsMonthData("Feb", BigDecimal("355"), BigDecimal("125"), BigDecimal("5100")),
            EpsMonthData("Mar", BigDecimal("345"), BigDecimal("115"), BigDecimal("4950")),
            EpsMonthData("Apr", BigDecimal("360"), BigDecimal("130"), BigDecimal("5150"))
        )

        val result = useCase(data)

        org.junit.Assert.assertTrue("Trend check", Trend.STABLE == result.trend)
        assertTrue(result.confidenceLevel > 0.5)
    }

    @Test
    fun `predict with increasing trend generates alert`() {
        val data = listOf(
            EpsMonthData("Jan", BigDecimal("350"), BigDecimal("120"), BigDecimal("4500")),
            EpsMonthData("Feb", BigDecimal("380"), BigDecimal("140"), BigDecimal("5000")),
            EpsMonthData("Mar", BigDecimal("420"), BigDecimal("160"), BigDecimal("5800")),
            EpsMonthData("Apr", BigDecimal("450"), BigDecimal("180"), BigDecimal("6500"))
        )

        val result = useCase(data)

        org.junit.Assert.assertTrue("Trend increase check", Trend.INCREASING == result.trend)
        assertTrue(result.alerts.any { it.type == AlertType.HIGH_CONSUMPTION })
    }

    @Test
    fun `predict detects unusual spike`() {
        val data = listOf(
            EpsMonthData("Jan", BigDecimal("350"), BigDecimal("120"), BigDecimal("5000")),
            EpsMonthData("Feb", BigDecimal("355"), BigDecimal("125"), BigDecimal("5100")),
            EpsMonthData("Mar", BigDecimal("345"), BigDecimal("115"), BigDecimal("4950")),
            EpsMonthData("Apr", BigDecimal("600"), BigDecimal("300"), BigDecimal("9000"))  // Spike!
        )

        val result = useCase(data)

        assertTrue(result.alerts.any { it.type == AlertType.UNUSUAL_SPIKE })
        assertTrue(result.alerts.any { it.severity == Severity.CRITICAL })
    }

    @Test
    fun `predict detects high VT NT ratio`() {
        val data = listOf(
            EpsMonthData("Jan", BigDecimal("400"), BigDecimal("50"), BigDecimal("5000")),  // 8:1 ratio
            EpsMonthData("Feb", BigDecimal("420"), BigDecimal("60"), BigDecimal("5200"))   // 7:1 ratio
        )

        val result = useCase(data)

        assertTrue(result.alerts.any { it.message.contains("VT/NT") })
    }

    @Test
    fun `linear prediction increases with growing data`() {
        val data = listOf(
            EpsMonthData("Jan", BigDecimal("350"), BigDecimal("120"), BigDecimal("5000")),
            EpsMonthData("Feb", BigDecimal("400"), BigDecimal("140"), BigDecimal("5500")),
            EpsMonthData("Mar", BigDecimal("450"), BigDecimal("160"), BigDecimal("6000"))
        )

        val result = useCase(data)

        // Predicted should be around 6500 (continuing the upward trend)
        assertTrue(result.predictedAmount > BigDecimal("6000"))
    }
}
