# 03 — Data Model

## Principles
Store only normalized fields needed for expense tracking. Store money as integer paise (`Long`). Do not retain raw notification text by default.

## Transaction entity (illustrative Room model)
```kotlin
@Entity(
    tableName = "transactions",
    indices = [Index("occurredAtEpochMillis"), Index("categoryId")]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amountPaise: Long,
    val currencyCode: String = "INR",
    val direction: String, // DEBIT, CREDIT
    val merchant: String?,
    val note: String?,
    val categoryId: String,
    val occurredAtEpochMillis: Long,
    val sourceType: String, // MANUAL, NOTIFICATION
    val sourcePackage: String?,
    val confidence: Float?,
    val reviewStatus: String, // CONFIRMED, NEEDS_REVIEW
    val dedupeKey: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
```
Add database-level uniqueness for dedupe keys if suitable for your Room version; nullable unique-index behavior should be verified. Alternatively maintain a separate deduplication table.

## Budget settings
```kotlin
@Entity(tableName = "budget_settings")
data class BudgetSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val budgetPeriod: String = "MONTHLY",
    val monthlyIncomePaise: Long?,
    val startingSpendableBalancePaise: Long?,
    val savingsReservePaise: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
```

## Commitments
```kotlin
@Entity(tableName = "commitments")
data class CommitmentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amountPaise: Long,
    val dueAtEpochMillis: Long,
    val isPaid: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
```

## Monitored apps
Store package name, UI display label, enabled flag and user selection timestamp in DataStore or Room. Use stable package names rather than display labels for filtering.

## Categories
FOOD, TRANSPORT, SHOPPING, BILLS, HEALTH, ENTERTAINMENT, INCOME, TRANSFER, OTHER.

## Parser diagnostics
Store only parser version, result/reason code, candidate ID and timestamp. Do not store raw notification body in audit records by default.

## Relationships and migrations
- Each transaction has one category.
- A budget may have many commitments.
- Use migrations for every schema change.
- Clear-data must remove transactions, budgets, commitments, preferences, dedupe fingerprints and diagnostics.

## Example CSV
```csv
date,direction,amount_inr,merchant,category,note,source
2026-10-22,DEBIT,350.00,Coffee Shop,FOOD,,NOTIFICATION
2026-10-22,CREDIT,25000.00,Salary,INCOME,Monthly salary,MANUAL
```
Exclude raw notification text, phone numbers, account identifiers and internal hashes from export by default.
