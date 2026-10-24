package com.spendwise.android.domain.calculator

import java.util.Calendar
import java.util.TimeZone

/**
 * Pure Kotlin calculator for Daily Spending Limits and Disciplined Financial Streaks.
 *
 * Rules:
 * - 100% Integer Paise (Long) for all financial arithmetic.
 * - Zero floating-point operations.
 * - A day is considered "Disciplined" if total expenses for that calendar day are <= daily limit.
 * - Current streak is the number of consecutive disciplined days immediately preceding and including today (if today is within limit).
 * - If today's spend exceeds the daily limit, current streak resets to 0.
 * - If past days have breaches, the streak resets to 0 starting from the breach date.
 * - Best streak is the maximum consecutive disciplined streak across all recorded history.
 */
data class DaySpendSummary(
    val dayEpochMillis: Long,
    val spentPaise: Long
)

data class DailyLimitStatus(
    val spentPaise: Long,
    val limitPaise: Long,
    val isWithinLimit: Boolean,
    val remainingPaise: Long,
    val overspentPaise: Long
)

data class StreakEvaluation(
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val dailyLimitStatus: DailyLimitStatus
)

object StreakCalculator {

    /**
     * Evaluates daily spending limit adherence for a single day.
     */
    fun evaluateDailyLimit(spentPaise: Long, limitPaise: Long): DailyLimitStatus {
        require(limitPaise > 0L) { "Daily limit must be strictly positive integer paise" }
        require(spentPaise >= 0L) { "Spent amount cannot be negative paise" }

        val isWithin = spentPaise <= limitPaise
        val remaining = (limitPaise - spentPaise).coerceAtLeast(0L)
        val overspent = (spentPaise - limitPaise).coerceAtLeast(0L)

        return DailyLimitStatus(
            spentPaise = spentPaise,
            limitPaise = limitPaise,
            isWithinLimit = isWithin,
            remainingPaise = remaining,
            overspentPaise = overspent
        )
    }

    /**
     * Calculates current and best streak from historical daily spends.
     * @param historicalDays List of past daily records, sorted chronologically ascending.
     * @param todaySpentPaise Expenses recorded on current date.
     * @param dailyLimitPaise The target daily expenditure limit.
     */
    fun calculateStreak(
        historicalDays: List<DaySpendSummary>,
        todaySpentPaise: Long,
        dailyLimitPaise: Long
    ): StreakEvaluation {
        val todayStatus = evaluateDailyLimit(todaySpentPaise, dailyLimitPaise)

        // Evaluate historical days sequentially
        var runningStreak = 0
        var bestStreak = 0

        for (day in historicalDays) {
            if (day.spentPaise <= dailyLimitPaise) {
                runningStreak++
                if (runningStreak > bestStreak) {
                    bestStreak = runningStreak
                }
            } else {
                runningStreak = 0
            }
        }

        // Incorporate today's status
        val currentStreak = if (todayStatus.isWithinLimit) {
            runningStreak + 1
        } else {
            0
        }

        if (currentStreak > bestStreak) {
            bestStreak = currentStreak
        }

        return StreakEvaluation(
            currentStreakDays = currentStreak,
            bestStreakDays = bestStreak,
            dailyLimitStatus = todayStatus
        )
    }

    /**
     * Helper to compute start and end epoch milliseconds for an exact calendar day.
     */
    fun getDayEpochRange(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Pair<Long, Long> {
        val cal = Calendar.getInstance(timeZone)
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    /**
     * Helper to compute start and end epoch milliseconds for a full calendar month.
     */
    fun getMonthEpochRange(
        year: Int,
        month: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Pair<Long, Long> {
        val cal = Calendar.getInstance(timeZone)
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, maxDay)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }
}
