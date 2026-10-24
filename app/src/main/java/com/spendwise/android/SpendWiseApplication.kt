package com.spendwise.android

import android.app.Application
import com.spendwise.android.data.db.AppDatabase
import com.spendwise.android.data.repository.BudgetRepository
import com.spendwise.android.data.repository.TransactionRepository

/**
 * Base Application class for SpendWise.
 * Initializes the Room database and repository singletons.
 */
class SpendWiseApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(database.transactionDao())
    }

    val spendoFinancialRepository: com.spendwise.android.data.repository.SpendoFinancialRepository by lazy {
        com.spendwise.android.data.repository.SpendoFinancialRepository(database)
    }

    val budgetRepository: BudgetRepository by lazy {
        BudgetRepository(
            budgetSettingsDao = database.budgetSettingsDao(),
            commitmentDao = database.commitmentDao(),
            transactionDao = database.transactionDao()
        )
    }

    val monitoredAppRepository: com.spendwise.android.data.repository.MonitoredAppRepository by lazy {
        com.spendwise.android.data.repository.MonitoredAppRepository(database.monitoredAppDao())
    }

    val notificationSettingsRepository: com.spendwise.android.data.repository.NotificationSettingsRepository by lazy {
        com.spendwise.android.data.repository.NotificationSettingsRepository(this)
    }

    val notificationAccessManager: com.spendwise.android.notification.NotificationAccessManager by lazy {
        com.spendwise.android.notification.NotificationAccessManager(this, notificationSettingsRepository)
    }

    val notificationConsumer: com.spendwise.android.notification.TransactionNotificationConsumer by lazy {
        com.spendwise.android.notification.NotificationIngestionEngine(database.transactionDao())
    }

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("SpendWiseCrash", "FATAL EXCEPTION on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
