package com.spendwise.android.util

import java.text.DecimalFormat
import java.util.Locale

/**
 * Utility for formatting Indian Rupee currency amounts stored strictly as integer paise.
 * In accordance with financial precision rules, no floating-point arithmetic is used for money calculations.
 */
object CurrencyFormatter {

    /**
     * Formats an amount in paise to an Indian Rupee string with symbol (e.g. ₹1,234.50).
     */
    fun formatPaise(paise: Long): String {
        val isNegative = paise < 0
        val absPaise = if (paise == Long.MIN_VALUE) Long.MAX_VALUE else Math.abs(paise)
        val rupees = absPaise / 100
        val remainingPaise = absPaise % 100

        // Format rupees with Indian numbering grouping (lakhs, crores) if possible
        val formattedRupees = formatIndianGrouping(rupees)
        val paiseStr = String.format(Locale.ROOT, "%02d", remainingPaise)

        val sign = if (isNegative) "-" else ""
        return "${sign}₹${formattedRupees}.${paiseStr}"
    }

    /**
     * Helper to format whole rupees with Indian thousand/lakh/crore commas (e.g. 12,34,567).
     */
    private fun formatIndianGrouping(rupees: Long): String {
        val s = rupees.toString()
        if (s.length <= 3) return s

        val lastThree = s.substring(s.length - 3)
        val remaining = s.substring(0, s.length - 3)

        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count % 2 == 0 && i > 0) {
                sb.append(',')
            }
        }
        val groupedRemaining = sb.reverse().toString()
        return "$groupedRemaining,$lastThree"
    }
}
