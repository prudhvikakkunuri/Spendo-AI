package com.spendwise.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isConcealed = prefs.getBoolean(KEY_PRIVACY_CONCEALED, false)

        CoroutineScope(Dispatchers.IO).launch {
            var totalBalancePaise = 0L
            var monthSpentPaise = 0L
            var todaySpentPaise = 0L
            var dailyBudgetLimitPaise: Long? = null
            var recentTxText = "No transactions recorded"
            var bankBreakdownText = "No accounts configured"
            var accountsSummaryText = "No accounts configured"

            try {
                val db = AppDatabase.getInstance(context)

                // 1. Bank Accounts & Balance
                val bankEntities = db.bankAccountDao().getAllBankAccountsDirect()
                totalBalancePaise = bankEntities.sumOf { it.balancePaise }

                if (bankEntities.isNotEmpty()) {
                    accountsSummaryText = bankEntities.take(3).joinToString(" • ") { it.accountNickname ?: it.bankName }
                    bankBreakdownText = bankEntities.take(3).joinToString(" • ") { bank ->
                        val name = bank.accountNickname ?: bank.bankName
                        val bal = bank.balancePaise / 100.0
                        val balStr = if (bal >= 1000) "₹%.1fk".format(Locale.ENGLISH, bal / 1000.0) else "₹%.0f".format(Locale.ENGLISH, bal)
                        "$name $balStr"
                    }
                }

                // 2. Date ranges
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfToday = calendar.timeInMillis
                val endOfToday = startOfToday + 86399999L

                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startOfMonth = calendar.timeInMillis

                // 3. Debits for today and this month
                todaySpentPaise = db.transactionDao().getDebitsInDateRangeDirect(startOfToday, endOfToday)
                monthSpentPaise = db.transactionDao().getDebitsInDateRangeDirect(startOfMonth, endOfToday)

                // 4. Budget
                val dailyBudgetEntity = db.budgetDao().getOverallBudgetByPeriodDirect("DAILY")
                dailyBudgetLimitPaise = dailyBudgetEntity?.limitPaise

                // 5. Recent Transaction
                val recentTransactions = db.transactionDao().getConfirmedTransactionsDirect()
                val recentTx = recentTransactions.firstOrNull()
                if (recentTx != null) {
                    val merchant = recentTx.merchant ?: "Transaction"
                    val sign = if (recentTx.direction == "CREDIT") "+" else "−"
                    val amt = "₹%,.2f".format(Locale.ENGLISH, recentTx.amountPaise / 100.0)
                    recentTxText = "$merchant • $sign$amt"
                }
            } catch (e: Exception) {
                // Safeguard against query failure during initialization
            }

            for (widgetId in appWidgetIds) {
                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 140)
                val isWide = minWidth >= 220

                val views = if (isWide) {
                    RemoteViews(context.packageName, R.layout.widget_spendo_4x2).apply {
                        if (isConcealed) {
                            setTextViewText(R.id.widget_4x2_total_balance, "••••••")
                            setTextViewText(R.id.widget_4x2_privacy_toggle, "REVEAL")
                            setTextViewText(R.id.widget_4x2_bank_breakdown, "Protected Balance")
                            setTextViewText(R.id.widget_4x2_today_spend, "Today: ••••••")
                            setTextViewText(R.id.widget_4x2_month_spend, "Month: ••••••")
                        } else {
                            setTextViewText(R.id.widget_4x2_total_balance, "₹%,.2f".format(Locale.ENGLISH, totalBalancePaise / 100.0))
                            setTextViewText(R.id.widget_4x2_privacy_toggle, "CONCEAL")
                            setTextViewText(R.id.widget_4x2_bank_breakdown, bankBreakdownText)
                            setTextViewText(R.id.widget_4x2_today_spend, "Today: ₹%,.2f".format(Locale.ENGLISH, todaySpentPaise / 100.0))
                            setTextViewText(R.id.widget_4x2_month_spend, "Month: ₹%,.2f".format(Locale.ENGLISH, monthSpentPaise / 100.0))
                        }

                        setTextViewText(R.id.widget_4x2_recent_tx, recentTxText)

                        // Intents
                        setOnClickPendingIntent(R.id.widget_4x2_privacy_toggle, getPrivacyTogglePendingIntent(context))
                        setOnClickPendingIntent(R.id.widget_4x2_refresh, getRefreshPendingIntent(context))
                        setOnClickPendingIntent(R.id.widget_4x2_root, getAppOpenPendingIntent(context))
                        setOnClickPendingIntent(R.id.widget_4x2_action_add, getAddExpensePendingIntent(context))
                        setOnClickPendingIntent(R.id.widget_4x2_action_voice, getVoiceExpensePendingIntent(context))
                        setOnClickPendingIntent(R.id.widget_4x2_action_ai, getSpendoAiPendingIntent(context))
                    }
                } else {
                    RemoteViews(context.packageName, R.layout.widget_spendo_2x2).apply {
                        if (isConcealed) {
                            setTextViewText(R.id.widget_2x2_today_spend, "••••••")
                            setTextViewText(R.id.widget_2x2_privacy_toggle, "SHOW")
                            setTextViewText(R.id.widget_2x2_total_balance, "Budget: ••••••")
                            setTextViewText(R.id.widget_2x2_accounts_summary, "Balances Protected")
                        } else {
                            setTextViewText(R.id.widget_2x2_today_spend, "₹%,.2f".format(Locale.ENGLISH, todaySpentPaise / 100.0))
                            setTextViewText(R.id.widget_2x2_privacy_toggle, "HIDE")

                            if (dailyBudgetLimitPaise != null && dailyBudgetLimitPaise > 0L) {
                                val remainingBudget = (dailyBudgetLimitPaise - todaySpentPaise).coerceAtLeast(0L)
                                setTextViewText(
                                    R.id.widget_2x2_total_balance,
                                    "Budget: ₹%,.0f left".format(Locale.ENGLISH, remainingBudget / 100.0)
                                )
                            } else {
                                setTextViewText(
                                    R.id.widget_2x2_total_balance,
                                    "Bal: ₹%,.2f".format(Locale.ENGLISH, totalBalancePaise / 100.0)
                                )
                            }

                            setTextViewText(R.id.widget_2x2_accounts_summary, accountsSummaryText)
                        }

                        // Intents
                        setOnClickPendingIntent(R.id.widget_2x2_privacy_toggle, getPrivacyTogglePendingIntent(context))
                        setOnClickPendingIntent(R.id.widget_2x2_root, getAppOpenPendingIntent(context))
                        setOnClickPendingIntent(R.id.widget_2x2_action_add, getAddExpensePendingIntent(context))
                        setOnClickPendingIntent(R.id.widget_2x2_action_voice, getVoiceExpensePendingIntent(context))
                    }
                }

                appWidgetManager.updateAppWidget(widgetId, views)
            }
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
