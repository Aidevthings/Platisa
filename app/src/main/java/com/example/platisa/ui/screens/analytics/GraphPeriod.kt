package com.example.platisa.ui.screens.analytics

enum class GraphPeriod {
    MONTHLY,
    SIX_MONTHS,
    THIS_YEAR
}

fun isDateInGraphPeriod(date: java.util.Date, period: GraphPeriod): Boolean {
    val calendar = java.util.Calendar.getInstance()
    val currentYear = calendar.get(java.util.Calendar.YEAR)
    val currentMonth = calendar.get(java.util.Calendar.MONTH)

    val targetCal = java.util.Calendar.getInstance()
    targetCal.time = date
    val targetYear = targetCal.get(java.util.Calendar.YEAR)
    val targetMonth = targetCal.get(java.util.Calendar.MONTH)

    return when (period) {
        GraphPeriod.MONTHLY -> {
            targetYear == currentYear && targetMonth == currentMonth
        }
        GraphPeriod.SIX_MONTHS -> {
            val monthsDiff = (currentYear - targetYear) * 12 + (currentMonth - targetMonth)
            monthsDiff in 0..5
        }
        GraphPeriod.THIS_YEAR -> {
            targetYear == currentYear
        }
    }
}
