# SpendWise Android

**SpendWise** is an Android-first, privacy-hardened personal expense tracker built with native Kotlin, Jetpack Compose, Material 3, and Room. It detects likely debit and credit transactions locally from notifications posted by user-approved banking and payment applications, records expenses on-device, categorizes transactions, calculates monthly budgets, and provides daily Safe-to-Spend insights.

---

## 🚀 Status: Phase 0–6 Complete (Production-Ready Architecture)

SpendWise has undergone end-to-end implementation and quality assurance:
- **Phase 0:** Project Foundation & Modular Clean Architecture
- **Phase 1:** Compose UI, Deep Charcoal & Lime Design System, 4-Tab Navigation
- **Phase 2:** Room Database, Strict Integer Paise Ledger, Filters, CRUD & Sanitized CSV Export
- **Phase 3:** Pure Kotlin `SafeToSpendCalculator`, Budget Profiles, Commitments & Safe-to-Spend
- **Phase 4:** `NotificationListenerService` Ingestion, In-Memory Allowlist Filtering & Instant Pause Switch
- **Phase 5:** Deterministic Heuristic `TransactionParser`, Reversals/Duplicates, and Review Queue
- **Phase 6:** Quality Assurance, Security Hardening, Backup Exclusion, and Privacy Verification

---

## 🔒 Non-Negotiable Privacy & Security Guarantees

SpendWise is designed from the ground up with a strict privacy-first model:

1. **Zero Network Communication:**
   - The application does **not** declare the `android.permission.INTERNET` permission in its manifest.
   - Financial data, parsed summaries, and user inputs can never physically leave the device.
2. **No Bank Credentials, UPI APIs, or SMS Reading:**
   - SpendWise never prompts for bank logins, UPI PINs, account credentials, or `RECEIVE_SMS`/`READ_SMS` permissions.
3. **Explicit User-Granted Notification Access:**
   - Ingestion relies strictly on Android's native `NotificationListenerService`.
   - Requires explicit one-time activation in Android system settings with a dedicated onboarding explanation.
4. **Strict Package Allowlisting:**
   - Only notifications from user-selected payment and banking packages (e.g. Google Pay, PhonePe, Paytm, CRED, BHIM, major Indian banks) are inspected. Notifications from all other apps are discarded immediately at the OS callback level.
5. **No Cloud Sync, Analytics SDKs, or External AI:**
   - Zero third-party telemetry, trackers, external LLMs, or crash-reporting servers.
6. **No Raw Financial Log Leakage:**
   - Android logs (`Logcat`) never contain raw notification bodies, full merchant names, or bank balance numbers. Log calls only output non-sensitive status codes and package IDs.
7. **Cloud Backup Exclusions:**
   - `android:allowBackup="false"` is enforced.
   - `backup_rules.xml` and `data_extraction_rules.xml` explicitly exclude the Room database (`database`) and DataStore preferences (`sharedpref`) from automated Android cloud backups.
8. **Instant Pause Switch & Clear All Data:**
   - Toggling **Pause Detection** immediately halts ingestion in memory.
   - **Clear All Data** purges transactions, budget settings, recurring commitments, deduplication tables, and preferences.
9. **Sanitized CSV Ledger Export:**
   - Exported CSV files contain clean structured accounting columns (`date,direction,amount_inr,merchant,category,note,source`). No raw notification text or internal database IDs are ever exported.

---

## 💡 Tech Stack

- **Language:** Kotlin 1.9.24
- **UI Toolkit:** Jetpack Compose with Material 3 (Charcoal `#121212`, Surface `#1E1E1E`, Lime `#C6FF00`)
- **Architecture:** Unidirectional Data Flow (UDF), MVVM (`ViewModel` + `StateFlow`), Coroutines
- **Local Database:** Room 2.6.1 with SQLite
- **Local Preferences:** AndroidX DataStore (Preferences)
- **Minimum SDK:** API 26 (Android 8.0 Oreo)
- **Target SDK:** API 34 (Android 14 UpsideDownCake)
- **Toolchain:** Android Gradle Plugin 8.3.2, Gradle 8.6, Java 17

```text
com.spendwise.android
├── data/
│   ├── db/
│   │   ├── entity/             # TransactionEntity, BudgetProfileEntity, RecurringCommitmentEntity
│   │   ├── dao/                # TransactionDao, BudgetDao, RecurringCommitmentDao
│   │   └── AppDatabase.kt      # Room database with pre-populated default budget
│   ├── preferences/            # SpendWisePreferencesRepository (allowlists, pause state)
│   └── repository/             # TransactionRepositoryImpl, BudgetRepositoryImpl
├── domain/
│   ├── model/                  # Domain transactions, directions, categories, review statuses
│   ├── parser/                 # Pure Kotlin TransactionParser (heuristic rules & regexes)
│   ├── budget/                 # Pure Kotlin SafeToSpendCalculator
│   └── dedup/                  # TransactionDeduplicator (multi-signal fingerprinting)
├── notification/               # TransactionNotificationListenerService & engine bridge
├── ui/
│   ├── theme/                  # Material 3 Charcoal & Lime theme, typography, color tokens
│   ├── navigation/             # Bottom navigation destinations & routing
│   ├── dashboard/              # Safe-to-spend hero card, today's spending, category breakdowns
│   ├── transactions/           # Transaction ledger, search, filters, manual entry dialog
│   ├── review/                 # Review Queue for ambiguous or duplicate transactions
│   ├── budget/                 # Income, savings reserve, upcoming recurring commitments
│   └── settings/               # Monitored apps selection, detection pause, export & wipe
└── util/                       # CurrencyFormatter (deterministic paise to INR), CsvExporter
```

---

## 💰 Financial Core & Money Arithmetic

SpendWise enforces **strict integer paise (`Long`)** for all monetary values.
- `₹1.00` is represented as `100L` paise.
- **Never uses `Float` or `Double`** for currency arithmetic, eliminating IEEE-754 floating-point rounding errors.
- **Safe-to-Spend Calculation:**
  $$\text{Spendable Remaining} = \text{Monthly Income} - \text{Savings Reserve} - \text{Upcoming Commitments} - \text{Confirmed Month Debits}$$
  $$\text{Daily Safe-to-Spend} = \frac{\max(0, \text{Spendable Remaining})}{\text{Days Remaining in Month}}$$
- **Review Queue Isolation:** Unconfirmed transactions (`NEEDS_REVIEW`) and internal transfers are excluded from confirmed monthly spending until explicitly approved by the user.

---

## 🔍 Supported Transaction Formats & Heuristics

The on-device parser detects standard Indian payment notifications across UPI, net banking, debit cards, and credit cards:

### Supported Formats:
- `Rs. 450.00`, `INR 1,250`, `₹750.50`, `INR 2500.00`
- Balance exclusion safeguards: ignores `Bal: ₹12,400.00`, `Avl Bal INR 5,000`, or `Account Balance is Rs. 10,000`

### Direction Heuristics:
- **Debit:** "debited by", "paid to", "spent on", "sent to", "withdrawn from", "payment to"
- **Credit:** "credited with", "received from", "refund of", "cashback of", "deposited to"

### Robust Boundary Handling:
- **Reversals / Refunds:** Automatically classified with high confidence into the `INCOME` / credit category.
- **Failed / Declined Transactions:** Flagged as `FAILED_PAYMENT` and routed to the Review Queue with `NEEDS_REVIEW` status.
- **Ambiguous / Multi-Amount Notifications:** Flagged as `AMBIGUOUS_AMOUNT` and routed to the Review Queue for manual verification.
- **Non-Transaction Noise:** Informational OTPs, promotional campaigns, and loan offers are silently ignored (`NotTransaction`).
- **Deduplication:** Short-window fingerprinting prevents double-recording when both a bank SMS app and a UPI app notify for the same event.

---

## ⚠️ Known Limitations

1. **Language Support:** Detection heuristics currently target English-language banking and UPI notifications. Regional Indian language notifications are ignored.
2. **Notification Clearing:** If the Android OS kills notification listeners under aggressive battery-saver OEM profiles before delivery, notifications cannot be retroactively fetched without Accessibility or SMS permissions (which SpendWise strictly refuses to request).
3. **Multi-Leg EMI / Foreign Currencies:** Complex loan restructuring and non-INR foreign transactions are not automatically categorized.
4. **Manual Fallback:** For unmonitored apps, cash purchases, or edge-case transactions, users can instantly add transactions manually from the Transactions screen.

---

## 🛠️ Build & Verification Instructions

### Prerequisites
- **Java Development Kit:** JDK 17 LTS (`JAVA_HOME` pointing to JDK 17)
- **Android SDK:** Platform 34 and Build-Tools 34.0.0 (`ANDROID_HOME` pointing to SDK)

### 1. Run Unit Tests (6 Test Suites, 56+ Unit Tests)
```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
.\gradlew.bat test
```
Runs:
- `ProjectFoundationUnitTest` (Architecture & baseline models)
- `Phase1NavigationAndDemoUnitTest` (Navigation routes & theme validation)
- `Phase2LedgerAndCsvUnitTest` (Room entities, DAOs, and sanitized CSV exports)
- `Phase3BudgetAndSafeToSpendUnitTest` (Pure `SafeToSpendCalculator` and paise arithmetic)
- `Phase4NotificationInfrastructureUnitTest` (Package allowlisting and pause states)
- `Phase5TransactionParsingUnitTest` (Heuristic parsing, reversals, failures, deduplication)
- `Phase6QualityAssuranceUnitTest` (Privacy, ledger mathematical integrity, missing-input handling)

### 2. Assemble Debug APK
```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
.\gradlew.bat assembleDebug
```
Output APK location:
`app/build/outputs/apk/debug/app-debug.apk`

### 3. Install & Run via ADB
```powershell
adb devices
.\gradlew.bat installDebug
adb shell am start -n com.spendwise.android/.MainActivity
```
To enable notification access on an emulator or physical test device:
1. Open SpendWise.
2. Navigate to **Settings** > **Notification Access**.
3. Tap **Enable Notification Access** to launch Android's Notification Listener settings.
4. Grant permission to **SpendWise**.
5. Select monitored apps in **Monitored Apps Setup**.
