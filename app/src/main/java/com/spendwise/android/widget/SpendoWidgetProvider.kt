package com.spendwise.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.spendwise.android.MainActivity
import com.spendwise.android.R
import com.spendwise.android.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Genuine Nothing OS-inspired Android Home Screen Launcher Widget for Spendo AI.
 * Displays glanceable total liquid balances, daily debits, monthly spending, and budget progress.
 * Supports quick voice recording and manual expense entry without opening the main dashboard.
 */
class SpendoWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_PRIVACY = "com.spendwise.android.widget.ACTION_TOGGLE_PRIVACY"
        const val ACTION_REFRESH = "com.spendwise.android.widget.ACTION_REFRESH"
        private const val PREFS_NAME = "spendo_widget_prefs"
        private const val KEY_PRIVACY_CONCEALED = "is_balance_concealed"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SpendoWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            val intent = Intent(context, SpendoWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        renderWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        renderWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE_PRIVACY -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val current = prefs.getBoolean(KEY_PRIVACY_CONCEALED, false)
                prefs.edit().putBoolean(KEY_PRIVACY_CONCEALED, !current).apply()

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, SpendoWidgetProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(thisWidget)
                renderWidgets(context, appWidgetManager, ids)
            }
            ACTION_REFRESH -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, SpendoWidgetProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(thisWidget)
                renderWidgets(context, appWidgetManager, ids)
            }
        }
    }

    private fun renderWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            var todaySpentPaise = 0L
            var dailyBudgetLimitPaise: Long? = null
            var streak = 0
            var monthSpentPaise = 0L
            var totalLiquidBalancePaise = 0L
            var recentExpenses: List<WidgetExpenseItem> = emptyList()

            try {
                val db = AppDatabase.getInstance(context)

                // 1. Date boundaries for Today
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfToday = calendar.timeInMillis
                val endOfToday = startOfToday + 86399999L

                // 2. Total eligible expenses dated today
                todaySpentPaise = db.transactionDao().getDebitsInDateRangeDirect(startOfToday, endOfToday)

                // 3. Daily spending limit from budget or persistent preferences
                val dailyBudgetEntity = db.budgetDao().getOverallBudgetByPeriodDirect("DAILY")
                val dailyLimitPref = db.appPreferenceDao().getPreferenceValueDirect("spendo_daily_limit_paise")?.toLongOrNull()
                dailyBudgetLimitPaise = dailyBudgetEntity?.limitPaise ?: dailyLimitPref

                // 4. Consecutive calendar logging streak
                val allConfirmed = db.transactionDao().getConfirmedTransactionsDirect()
                val eligibleDebits = allConfirmed
                    .filter { it.direction == "DEBIT" && it.categoryId != "TRANSFER" && it.categoryId != "TRANSFERS" }
                val eligibleDates = eligibleDebits.map { it.occurredAtEpochMillis }
                streak = com.spendwise.android.domain.calculator.StreakCalculator.calculateLoggingStreak(eligibleDates)

                // 5. Month-to-date spending
                val monthCal = Calendar.getInstance()
                monthCal.set(Calendar.DAY_OF_MONTH, 1)
                monthCal.set(Calendar.HOUR_OF_DAY, 0)
                monthCal.set(Calendar.MINUTE, 0)
                monthCal.set(Calendar.SECOND, 0)
                monthCal.set(Calendar.MILLISECOND, 0)
                val startOfMonth = monthCal.timeInMillis
                monthSpentPaise = db.transactionDao().getDebitsInDateRangeDirect(startOfMonth, System.currentTimeMillis())

                // 6. Liquid account balances
                val allBanks = db.bankAccountDao().getAllBankAccountsDirect()
                totalLiquidBalancePaise = allBanks.sumOf { it.balancePaise }

                // 7. Recent genuine debit expenses (up to 3)
                val timeFormat = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault())
                val dateFormat = java.text.SimpleDateFormat("dd MMM", Locale.getDefault())
                val todayStartMs = startOfToday

                val recentList = eligibleDebits
                    .sortedByDescending { it.occurredAtEpochMillis }
                    .take(3)
                    .map { tx ->
                        val cleanMerchant = tx.merchant?.trim()?.ifBlank { null }
                            ?: tx.note?.trim()?.ifBlank { null }
                            ?: "Expense"
                        val timeStr = if (tx.occurredAtEpochMillis >= todayStartMs) {
                            "Today, " + timeFormat.format(java.util.Date(tx.occurredAtEpochMillis))
                        } else {
                            dateFormat.format(java.util.Date(tx.occurredAtEpochMillis))
                        }
                        WidgetExpenseItem(cleanMerchant, tx.amountPaise, timeStr)
                    }
                recentExpenses = recentList
            } catch (e: Exception) {
                android.util.Log.e("SpendoWidget", "Error querying database for widget: ${e.message}")
            }

            val limitPaise = dailyBudgetLimitPaise
            val progressPercent = if (limitPaise != null && limitPaise > 0L) {
                ((todaySpentPaise.toFloat() / limitPaise) * 100f).toInt().coerceIn(0, 100)
            } else {
                0
            }

            val limitStatusText = if (limitPaise != null && limitPaise > 0L) {
                if (todaySpentPaise <= limitPaise) {
                    val remaining = limitPaise - todaySpentPaise
                    "₹%,.0f left of ₹%,.0f limit".format(Locale.ENGLISH, remaining / 100.0, limitPaise / 100.0)
                } else {
                    val over = todaySpentPaise - limitPaise
                    "Exceeded by ₹%,.0f".format(Locale.ENGLISH, over / 100.0)
                }
            } else {
                "Set daily spending limit"
            }

            val streakText = "🔥 $streak ${if (streak == 1) "DAY" else "DAYS"}"
            val streakStatusText = if (streak > 0) {
                "Streak active • $streak consecutive ${if (streak == 1) "day" else "days"}"
            } else {
                "Log expenses daily to build streak"
            }

            // Build RemoteViews variants
            val rv2x2 = build2x2Views(context, todaySpentPaise, progressPercent, limitStatusText, streakText)
            val rv2x3 = build2x3Views(context, todaySpentPaise, progressPercent, limitStatusText, streakText, streakStatusText)
            val rv3x3 = build3x3Views(context, todaySpentPaise, progressPercent, limitStatusText, streakText, streakStatusText, recentExpenses)
            val rv2x4 = build2x4Views(context, todaySpentPaise, progressPercent, limitStatusText, streakText, streakStatusText, monthSpentPaise, recentExpenses)
            val rv3x4 = build3x4Views(context, todaySpentPaise, progressPercent, limitStatusText, streakText, streakStatusText, monthSpentPaise, totalLiquidBalancePaise, recentExpenses)
            val rv4x2 = build4x2Views(context, todaySpentPaise, progressPercent, limitStatusText, streakText, streak)

            for (widgetId in appWidgetIds) {
                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 140)
                val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
                val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight)
                val effectiveHeight = maxOf(minHeight, maxHeight)

                val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        RemoteViews(
                            mapOf(
                                SizeF(110f, 100f) to rv2x2,
                                SizeF(220f, 100f) to rv4x2,
                                SizeF(110f, 150f) to rv2x3,
                                SizeF(180f, 150f) to rv3x3,
                                SizeF(110f, 220f) to rv2x4,
                                SizeF(180f, 220f) to rv3x4
                            )
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("SpendoWidget", "Error creating responsive RemoteViews: ${e.message}")
                        selectViewsByDimensions(minWidth, effectiveHeight, rv2x2, rv2x3, rv3x3, rv2x4, rv3x4, rv4x2)
                    }
                } else {
                    selectViewsByDimensions(minWidth, effectiveHeight, rv2x2, rv2x3, rv3x3, rv2x4, rv3x4, rv4x2)
                }

                try {
                    appWidgetManager.updateAppWidget(widgetId, views)
                } catch (e: Exception) {
                    android.util.Log.e("SpendoWidget", "Error updating widget id $widgetId: ${e.message}")
                    // Safe fallback to compact 2x2
                    try {
                        appWidgetManager.updateAppWidget(widgetId, rv2x2)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    data class WidgetExpenseItem(
        val merchant: String,
        val amountPaise: Long,
        val timeFormatted: String
    )

    private fun selectViewsByDimensions(
        minWidth: Int,
        effectiveHeight: Int,
        rv2x2: RemoteViews,
        rv2x3: RemoteViews,
        rv3x3: RemoteViews,
        rv2x4: RemoteViews,
        rv3x4: RemoteViews,
        rv4x2: RemoteViews
    ): RemoteViews {
        return when {
            effectiveHeight >= 220 -> if (minWidth >= 180) rv3x4 else rv2x4
            effectiveHeight >= 150 -> if (minWidth >= 180) rv3x3 else rv2x3
            minWidth >= 220 -> rv4x2
            else -> rv2x2
        }
    }

    private fun build2x2Views(
        context: Context,
        todaySpentPaise: Long,
        progressPercent: Int,
        limitStatusText: String,
        streakText: String
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_spendo_2x2).apply {
            setTextViewText(R.id.widget_2x2_today_spend, "₹%,.2f".format(Locale.ENGLISH, todaySpentPaise / 100.0))
            setTextViewText(R.id.widget_2x2_streak, streakText)
            setProgressBar(R.id.widget_2x2_progress_bar, 100, progressPercent, false)
            setTextViewText(R.id.widget_2x2_limit_status, limitStatusText)

            setOnClickPendingIntent(R.id.widget_2x2_root, getAppOpenPendingIntent(context))
            setOnClickPendingIntent(R.id.widget_2x2_action_add, getAddExpensePendingIntent(context))
            setOnClickPendingIntent(R.id.widget_2x2_action_voice, getVoiceExpensePendingIntent(context))
        }
    }

    private fun build2x3Views(
        context: Context,
        todaySpentPaise: Long,
        progressPercent: Int,
        limitStatusText: String,
        streakText: String,
        streakStatusText: String
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_spendo_2x3).apply {
            setTextViewText(R.id.widget_2x3_today_spend, "₹%,.2f".format(Locale.ENGLISH, todaySpentPaise / 100.0))
            setTextViewText(R.id.widget_2x3_streak, streakText)
            setProgressBar(R.id.widget_2x3_progress_bar, 100, progressPercent, false)
            setTextViewText(R.id.widget_2x3_limit_status, limitStatusText)
            setTextViewText(R.id.widget_2x3_streak_status, streakStatusText)

            setOnClickPendingIntent(R.id.widget_2x3_root, getAppOpenPendingIntent(context))
            setOnClickPendingIntent(R.id.widget_2x3_action_add, getAddExpensePendingIntent(context))
            setOnClickPendingIntent(R.id.widget_2x3_action_voice, getVoiceExpensePendingIntent(context))
        }
    }

    private fun build3x3Views(
        context: Context,
        todaySpentPaise: Long,
        progressPercent: Int,
        limitStatusText: String,
        streakText: String,
        streakStatusText: String,
        recentExpenses: List<WidgetExpenseItem>
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_spendo_3x3).apply {
            setTextViewText(R.id.widget_3x3_today_spend, "₹%,.2f".format(Locale.ENGLISH, todaySpentPaise / 100.0))
            setTextViewText(R.id.widget_3x3_streak, streakText)
            setProgressBar(R.id.widget_3x3_progress_bar, 100, progressPercent, false)
            setTextViewText(R.id.widget_3x3_limit_status, limitStatusText)

            if (recentExpenses.isEmpty()) {
                setViewVisibility(R.id.widget_3x3_recent_empty, android.view.View.VISIBLE)
                setViewVisibility(R.id.widget_3x3_recent_item_1, android.view.View.GONE)
            } else {
                val first = recentExpenses[0]
                setViewVisibility(R.id.widget_3x3_recent_empty, android.view.View.GONE)
                setViewVisibility(R.id.widget_3x3_recent_item_1, android.view.View.VISIBLE)
                setTextViewText(R.id.widget_3x3_recent_1_merchant, first.merchant)
                setTextViewText(R.id.widget_3x3_recent_1_amount, "-₹%,.2f".format(Locale.ENGLISH, first.amountPaise / 100.0))
                setTextViewText(R.id.widget_3x3_recent_1_time, first.timeFormatted)
            }

            setOnClickPendingIntent(R.id.widget_3x3_root, getAppOpenPendingIntent(context))
            setOnClickPendingIntent(R.id.widget_3x3_refresh, getRefreshPendingIntent(context))
            setOnClickPendingIntent(R.id.widget_3x3_action_add, getAddExpensePendingIntent(context))
            setOnClickPendingIntent(R.id.widget_3x3_action_voice, getVoiceExpensePendingIntent(context))
        }
    }

    private fun build2x4Views(
        context: Context,
        todaySpentPaise: Long,
        progressPercent: Int,
        limitStatusText: String,
        streakText: String,
        streakStatusText: String,
        monthSpentPaise: Long,
        recentExpenses: List<WidgetExpenseItem>
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_spendo_2x4).apply {
            setTextViewText(R.id.widget_2x4_today_spend, "₹%,.2f".format(Locale.ENGLISH, todaySpentPaise / 100.0))
            setTextViewText(R.id.widget_2x4_streak, streakText)
            setProgressBar(R.id.widget_2x4_progress_bar, 100, progressPercent, false)
            setTextViewText(R.id.widget_2x4_limit_status, limitStatusText)

            if (recentExpenses.isEmpty()) {
                setViewVisibility(R.id.widget_2x4_recent_empty, android.view.View.VISIBLE)
                setViewVisibility(R.id.widget_2x4_recent_item_1, android.view.View.GONE)
                setViewVisibility(R.id.widget_2x4_recent_item_2, android.view.View.GONE)
                setViewVisibility(R.id.widget_2x4_recent_item_3, android.view.View.GONE)
            } else {
                setViewVisibility(R.id.widget_2x4_recent_empty, android.view.View.GONE)
                val items = listOf(
                    Triple(R.id.widget_2x4_recent_item_1, R.id.widget_2x4_recent_1_merchant, Pair(R.id.widget_2x4_recent_1_time, R.id.widget_2x4_recent_1_amount)),
                    Triple(R.id.widget_2x4_recent_item_2, R.id.widget_2x4_recent_2_merchant, Pair(R.id.widget_2x4_recent_2_time, R.id.widget_2x4_recent_2_amount)),
                    Triple(R.id.widget_2x4_recent_item_3, R.id.widget_2x4_recent_3_merchant, Pair(R.id.widget_2x4_recent_3_time, R.id.widget_2x4_recent_3_amount))
                )
                items.forEachIndexed { index, (containerId, merchantId, timeAmountPair) ->
                    if (index < recentExpenses.size) {
                        val item = recentExpenses[index]
                        setViewVisibility(containerId, android.view.View.VISIBLE)
                        setTextViewText(merchantId, item.merchant)
                        setTextViewText(timeAmountPair.first, item.timeFormatted)
                        setTextViewText(timeAmountPair.second, "-₹%,.2f".format(Locale.ENGLISH, item.amountPaise / 100.0))
                    } else {
                        setViewVisibility(containerId, android.view.View.GONE)
                    }
                }
            }

            setOnClickPendingIntent(R.id.widget_2x4_root, getAppOpenPendingIntent(context))
            setOnClickPendingIntent(R.id.widget_2x4_refresh, getRefreshPendingIntent(context))
            setOnClickPendingIntent(R.id.widget_2x4_action_add, getAddExpensePendingIntent(context))
            setOnClickPendingIntent(R.id.widget_2x4_action_voice, getVoiceExpensePendingIntent(context))
        }
    }

    private fun build3x4Views(
        context: Context,
        todaySpentPaise: Long,
        progressPercent: Int,
        limitStatusText: String,
        streakText: String,
        streakStatusText: String,
        monthSpentPaise: Long,
        totalBalancePaise: Long,
        recentExpenses: List<WidgetExpenseItem>
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_spendo_3x4).apply {
            setTextViewText(R.id.widget_3x4_today_spend, "₹%,.2f".format(Locale.ENGLISH, todaySpentPaise / 100.0))
            setTextViewText(R.id.widget_3x4_streak, streakText)
            setProgressBar(R.id.widget_3x4_progress_bar, 100, progressPercent, false)
            setTextViewText(R.id.widget_3x4_limit_status, limitStatusText)
            setTextViewText(R.id.widget_3x4_month_spend, "₹%,.2f".format(Locale.ENGLISH, monthSpentPaise / 100.0))
            setTextViewText(R.id.widget_3x4_total_balance, "₹%,.2f".format(Locale.ENGLISH, totalBalancePaise / 100.0))
            setTextViewText(R.id.widget_3x4_streak_status, "🔥 $streakStatusText")

            if (recentExpenses.isEmpty()) {
                setViewVisibility(R.id.widget_3x4_recent_empty, android.view.View.VISIBLE)
                setViewVisibility(R.id.widget_3x4_recent_item_1, android.view.View.GONE)
                setViewVisibility(R.id.widget_3x4_recent_item_2, android.view.View.GONE)
                setViewVisibility(R.id.widget_3x4_recent_item_3, android.view.View.GONE)
            } else {
                setViewVisibility(R.id.widget_3x4_recent_empty, android.view.View.GONE)
                val items = listOf(
                    Triple(R.id.widget_3x4_recent_item_1, R.id.widget_3x4_recent_1_merchant, Pair(R.id.widget_3x4_recent_1_time, R.id.widget_3x4_recent_1_amount)),
                    Triple(R.id.widget_3x4_recent_item_2, R.id.widget_3x4_recent_2_merchant, Pair(R.id.widget_3x4_recent_2_time, R.id.widget_3x4_recent_2_amount)),
                    Triple(R.id.widget_3x4_recent_item_3, R.id.widget_3x4_recent_3_merchant, Pair(R.id.widget_3x4_recent_3_time, R.id.widget_3x4_recent_3_amount))
                )
                items.forEachIndexed { index, (containerId, merchantId, timeAmountPair) ->
                    if (index < recentExpenses.size) {
                        val item = recentExpenses[index]
                        setViewVisibility(containerId, android.view.View.VISIBLE)
                        setTextViewText(merchantId, item.merchant)
                        setTextViewText(timeAmountPair.first, item.timeFormatted)
                        setTextViewText(timeAmountPair.second, "-₹%,.2f".format(Locale.ENGLISH, item.amountPaise / 100.0))
                    } else {
                        setViewVisibility(containerId, android.view.View.GONE)
                    }
                }
            }

            setOnClickPendingIntent(R.id.widget_3x4_root, getAppOpenPendingIntent(context))
            setOnClickPendingIntent(R.id.widget_3x4_refresh, getRefreshPendingIntent(context))
            setOnClickPendingIntent(R.id.widget_3x4_action_add, getAddExpensePendingIntent(context))
            setOnClickPendingIntent(R.id.widget_3x4_action_ai, getVoiceExpensePendingIntent(context))
        }
    }

    private fun build4x2Views(
        context: Context,
        todaySpentPaise: Long,
        progressPercent: Int,
        limitStatusText: String,
        streakText: String,
        streak: Int
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_spendo_4x2).apply {
            setTextViewText(R.id.widget_4x2_today_spend, "₹%,.2f".format(Locale.ENGLISH, todaySpentPaise / 100.0))
            setTextViewText(R.id.widget_4x2_streak_badge, streakText)
            setTextViewText(R.id.widget_4x2_streak_number, "$streak ${if (streak == 1) "DAY" else "DAYS"}")
            setTextViewText(
                R.id.widget_4x2_streak_status,
                if (streak > 0) "Streak active • Log daily" else "Log today's expense to start"
            )
            setProgressBar(R.id.widget_4x2_progress_bar, 100, progressPercent, false)
            setTextViewText(R.id.widget_4x2_limit_status, limitStatusText)

            setOnClickPendingIntent(R.id.widget_4x2_refresh, getRefreshPendingIntent(context))
            setOnClickPendingIntent(R.id.widget_4x2_root, getAppOpenPendingIntent(context))
            setOnClickPendingIntent(R.id.widget_4x2_action_add, getAddExpensePendingIntent(context))
            setOnClickPendingIntent(R.id.widget_4x2_action_ai, getVoiceExpensePendingIntent(context))
        }
    }

    private fun getAppOpenPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getAddExpensePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, QuickExpenseActivity::class.java).apply {
            action = QuickExpenseActivity.ACTION_MANUAL
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getVoiceExpensePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, QuickExpenseActivity::class.java).apply {
            action = QuickExpenseActivity.ACTION_VOICE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            104,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getSpendoAiPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "spendo_ai")
        }
        return PendingIntent.getActivity(
            context,
            103,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getPrivacyTogglePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SpendoWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_PRIVACY
        }
        return PendingIntent.getBroadcast(
            context,
            201,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getRefreshPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SpendoWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        return PendingIntent.getBroadcast(
            context,
            202,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
