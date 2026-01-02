package com.platisa.app.core.common

import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val serbianLocale = Locale("sr", "RS")
    private val currencyFormat = NumberFormat.getNumberInstance(serbianLocale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun formatCurrency(amount: BigDecimal?): String {
        if (amount == null) return "0,00"
        return currencyFormat.format(amount)
    }
    
    fun formatCurrencyWithSuffix(amount: BigDecimal?, currency: String = "RSD"): String {
    val suffix = if (currency == "EUR") "€" else "RSD"
    return "${formatCurrency(amount)} $suffix"
}    
    // Format as whole number with thousand separators (dots) but no decimals
    fun formatCurrencySimple(amount: BigDecimal?): String {
        if (amount == null) return "0"
        val wholeNumber = amount.toLong()
        return NumberFormat.getNumberInstance(serbianLocale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }.format(wholeNumber)
    }

    fun formatAmount(amount: BigDecimal?): String {
        if (amount == null) return "0"
        return amount.stripTrailingZeros().toPlainString()
    }

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", serbianLocale)

    fun formatDate(date: Date?): String {
        if (date == null) return ""
        return dateFormat.format(date)
    }
}

