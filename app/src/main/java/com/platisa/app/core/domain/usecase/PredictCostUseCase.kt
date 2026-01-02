package com.platisa.app.core.domain.usecase

import com.platisa.app.core.domain.model.EpsMonthData
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class CostPrediction(
    val predictedAmount: BigDecimal,
    val confidenceLevel: Double, // 0.0 to 1.0
    val trend: Trend,
    val alerts: List<Alert>
)

enum class Trend {
    INCREASING,
    STABLE,
    DECREASING
}

data class Alert(
    val type: AlertType,
    val message: String,
    val severity: Severity
)

enum class AlertType {
    HIGH_CONSUMPTION,
    UNUSUAL_SPIKE,
    COST_INCREASE,
    PREDICTION_AVAILABLE
}

enum class Severity {
    INFO,
    WARNING,
    CRITICAL
}

class PredictCostUseCase @Inject constructor() {

    operator fun invoke(historicalData: List<EpsMonthData>): CostPrediction {
        if (historicalData.size < 2) {
            return CostPrediction(
                predictedAmount = BigDecimal.ZERO,
                confidenceLevel = 0.0,
                trend = Trend.STABLE,
                alerts = listOf(
                    Alert(
                        type = AlertType.PREDICTION_AVAILABLE,
                        message = "Nedovoljno podataka za predviđanje (potrebno min. 2 meseca)",
                        severity = Severity.INFO
                    )
                )
            )
        }

        // Simple linear regression on last N months
        val amounts = historicalData.map { it.totalAmount.toDouble() }
        val predicted = calculateLinearPrediction(amounts)
        val trend = analyzeTrend(amounts)
        val confidence = calculateConfidence(historicalData)
        val alerts = generateAlerts(historicalData, predicted, trend)

        return CostPrediction(
            predictedAmount = BigDecimal(predicted).setScale(2, RoundingMode.HALF_UP),
            confidenceLevel = confidence,
            trend = trend,
            alerts = alerts
        )
    }

    private fun calculateLinearPrediction(amounts: List<Double>): Double {
        val n = amounts.size
        val x = (0 until n).map { it.toDouble() }
        val y = amounts

        // Simple linear regression: y = mx + b
        val xMean = x.average()
        val yMean = y.average()

        val numerator = x.zip(y).sumOf { (xi, yi) -> (xi - xMean) * (yi - yMean) }
        val denominator = x.sumOf { xi -> (xi - xMean) * (xi - xMean) }

        val slope = if (denominator != 0.0) numerator / denominator else 0.0
        val intercept = yMean - slope * xMean

        // Predict next month (index = n)
        return slope * n + intercept
    }

    private fun analyzeTrend(amounts: List<Double>): Trend {
        if (amounts.size < 2) return Trend.STABLE

        val recent = amounts.takeLast(3)
        val older = amounts.dropLast(3).takeLast(3)

        if (older.isEmpty()) return Trend.STABLE

        val recentAvg = recent.average()
        val olderAvg = older.average()

        val changePercent = ((recentAvg - olderAvg) / olderAvg) * 100

        return when {
            changePercent > 10 -> Trend.INCREASING
            changePercent < -10 -> Trend.DECREASING
            else -> Trend.STABLE
        }
    }

    private fun calculateConfidence(data: List<EpsMonthData>): Double {
        // Confidence based on data points and variance
        val baseConfidence = when {
            data.size >= 6 -> 0.9
            data.size >= 4 -> 0.75
            data.size >= 2 -> 0.6
            else -> 0.3
        }

        // Reduce confidence if high variance
        val amounts = data.map { it.totalAmount.toDouble() }
        val mean = amounts.average()
        val variance = amounts.sumOf { (it - mean) * (it - mean) } / amounts.size
        val coefficientOfVariation = if (mean != 0.0) Math.sqrt(variance) / mean else 0.0

        val variancePenalty = when {
            coefficientOfVariation > 0.3 -> 0.2
            coefficientOfVariation > 0.2 -> 0.1
            else -> 0.0
        }

        return (baseConfidence - variancePenalty).coerceIn(0.0, 1.0)
    }

    private fun generateAlerts(
        data: List<EpsMonthData>,
        predicted: Double,
        trend: Trend
    ): List<Alert> {
        val alerts = mutableListOf<Alert>()

        // Alert if predicted cost is significantly higher than average
        val avgCost = data.map { it.totalAmount.toDouble() }.average()
        val increasePercent = ((predicted - avgCost) / avgCost) * 100

        if (increasePercent > 20) {
            alerts.add(
                Alert(
                    type = AlertType.COST_INCREASE,
                    message = "Predviđeni račun je ${increasePercent.toInt()}% viši od proseka (${predicted.toInt()} RSD)",
                    severity = Severity.WARNING
                )
            )
        }

        // Alert for unusual spike in last month
        if (data.size >= 2) {
            val lastMonth = data.last().totalAmount.toDouble()
            val previousAvg = data.dropLast(1).map { it.totalAmount.toDouble() }.average()
            val spikePercent = ((lastMonth - previousAvg) / previousAvg) * 100

            if (spikePercent > 30) {
                alerts.add(
                    Alert(
                        type = AlertType.UNUSUAL_SPIKE,
                        message = "Neobično visoka potrošnja prošlog meseca (+${spikePercent.toInt()}%)",
                        severity = Severity.CRITICAL
                    )
                )
            }
        }

        // Alert for high consumption trend
        if (trend == Trend.INCREASING) {
            alerts.add(
                Alert(
                    type = AlertType.HIGH_CONSUMPTION,
                    message = "Potrošnja raste - razmislite o energetskoj efikasnosti",
                    severity = Severity.INFO
                )
            )
        }

        // Check for high VT/NT ratio (inefficient usage)
        val lastMonth = data.lastOrNull()
        if (lastMonth != null) {
            val vtRatio = if (lastMonth.ntConsumption > BigDecimal.ZERO) {
                lastMonth.vtConsumption.divide(lastMonth.ntConsumption, 2, RoundingMode.HALF_UP).toDouble()
            } else {
                0.0
            }

            if (vtRatio > 3.0) {
                alerts.add(
                    Alert(
                        type = AlertType.HIGH_CONSUMPTION,
                        message = "Visok odnos VT/NT (${vtRatio.toInt()}:1) - koristite više noćnu tarifu",
                        severity = Severity.INFO
                    )
                )
            }
        }

        return alerts
    }
}

