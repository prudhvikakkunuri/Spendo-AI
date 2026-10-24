package com.spendwise.android.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spendwise.android.data.db.dao.AiProviderConfigDao
import com.spendwise.android.data.db.dao.AppPreferenceDao
import com.spendwise.android.data.db.dao.BankAccountDao
import com.spendwise.android.data.db.dao.BudgetDao
import com.spendwise.android.data.db.dao.BudgetSettingsDao
import com.spendwise.android.data.db.dao.CategoryDao
import com.spendwise.android.data.db.dao.CommitmentDao
import com.spendwise.android.data.db.dao.IncomeSourceDao
import com.spendwise.android.data.db.dao.MonitoredAppDao
import com.spendwise.android.data.db.dao.NotificationRecordDao
import com.spendwise.android.data.db.dao.TransactionDao
import com.spendwise.android.data.db.entity.AiProviderConfigEntity
import com.spendwise.android.data.db.entity.AppPreferenceEntity
import com.spendwise.android.data.db.entity.BankAccountEntity
import com.spendwise.android.data.db.entity.BudgetEntity
import com.spendwise.android.data.db.entity.BudgetSettingsEntity
import com.spendwise.android.data.db.entity.CategoryEntity
import com.spendwise.android.data.db.entity.CommitmentEntity
import com.spendwise.android.data.db.entity.IncomeSourceEntity
import com.spendwise.android.data.db.entity.MonitoredAppEntity
import com.spendwise.android.data.db.entity.NotificationRecordEntity
import com.spendwise.android.data.db.entity.TransactionEntity

/**
 * SpendWise / Spendo Room Database.
 * Local SQLite storage for normalized ledger transactions, budgets, bank accounts,
 * income sources, categories, notification detection logs, app preferences, and AI provider metadata.
 */
@Database(
    entities = [
        TransactionEntity::class,
        BudgetSettingsEntity::class,
        CommitmentEntity::class,
        MonitoredAppEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        BankAccountEntity::class,
        IncomeSourceEntity::class,
        NotificationRecordEntity::class,
        AppPreferenceEntity::class,
        AiProviderConfigEntity::class
    ],
    version = DatabaseContract.DATABASE_VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetSettingsDao(): BudgetSettingsDao
    abstract fun commitmentDao(): CommitmentDao
    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun incomeSourceDao(): IncomeSourceDao
    abstract fun notificationRecordDao(): NotificationRecordDao
    abstract fun appPreferenceDao(): AppPreferenceDao
    abstract fun aiProviderConfigDao(): AiProviderConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from version 1 to 2: adds budget_settings and commitments tables.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `budget_settings` (
                        `id` INTEGER NOT NULL,
                        `budget_period` TEXT NOT NULL,
                        `monthly_income_paise` INTEGER,
                        `starting_spendable_balance_paise` INTEGER,
                        `savings_reserve_paise` INTEGER NOT NULL,
                        `created_at_epoch_ms` INTEGER NOT NULL,
                        `updated_at_epoch_ms` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `commitments` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amount_paise` INTEGER NOT NULL,
                        `due_at_epoch_ms` INTEGER NOT NULL,
                        `is_paid` INTEGER NOT NULL,
                        `created_at_epoch_ms` INTEGER NOT NULL,
                        `updated_at_epoch_ms` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration from version 2 to 3: adds monitored_apps table for user-selected notification sources.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `monitored_apps` (
                        `package_name` TEXT NOT NULL,
                        `app_name` TEXT NOT NULL,
                        `is_enabled` INTEGER NOT NULL,
                        `created_at_epoch_ms` INTEGER NOT NULL,
                        `updated_at_epoch_ms` INTEGER NOT NULL,
                        PRIMARY KEY(`package_name`)
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration from version 3 to 4: adds bank account linking to transactions,
         * categories, budgets, bank_accounts, income_sources, notification_records,
         * app_preferences, and ai_provider_configs tables.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN bankAccountId TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN bankAccountName TEXT")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `iconName` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` TEXT NOT NULL,
                        `categoryId` TEXT,
                        `periodType` TEXT NOT NULL,
                        `limitPaise` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `bank_accounts` (
                        `id` TEXT NOT NULL,
                        `bankName` TEXT NOT NULL,
                        `accountType` TEXT NOT NULL,
                        `maskedAccountNumber` TEXT NOT NULL,
                        `balancePaise` INTEGER NOT NULL,
                        `openingBalancePaise` INTEGER NOT NULL,
                        `isPrimary` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `income_sources` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amountPaise` INTEGER NOT NULL,
                        `isRecurring` INTEGER NOT NULL,
                        `frequency` TEXT NOT NULL,
                        `nextPayDateEpochMs` INTEGER NOT NULL,
                        `bankAccountId` TEXT,
                        `category` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notification_records` (
                        `id` TEXT NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `appName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `detectedAmountPaise` INTEGER,
                        `direction` TEXT,
                        `category` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `postedAtEpochMs` INTEGER NOT NULL,
                        `progress` REAL,
                        `isUnread` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `app_preferences` (
                        `key` TEXT NOT NULL,
                        `value` TEXT NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`key`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ai_provider_configs` (
                        `providerId` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `selectedModel` TEXT NOT NULL,
                        `isConfigured` INTEGER NOT NULL,
                        `lastTestedEpochMs` INTEGER,
                        `lastTestMessage` TEXT,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`providerId`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4
        )

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DatabaseContract.DATABASE_NAME
                )
                    .addMigrations(*MIGRATIONS)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
