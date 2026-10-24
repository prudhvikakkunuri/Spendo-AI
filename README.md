<<<<<<< HEAD
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
=======
# Spendo AI

<p align="center">
  <strong>Personal finance, made clearer.</strong><br/>
  An AI-powered mobile experience for tracking expenses, understanding spending, and building better money habits.
</p>

<p align="center">
  <img alt="Project status" src="https://img.shields.io/badge/status-in%20development-orange" />
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android%20focused-3DDC84?logo=android&logoColor=white" />
  <img alt="Repository visibility" src="https://img.shields.io/badge/repository-public-blue" />
</p>

---

## Overview

**Spendo AI** is a personal finance application in development that aims to bring everyday money management and AI assistance into one focused mobile experience. The project is being built around a simple goal: help people understand where their money goes without making money management feel complicated.

This repository is the central home for the application's source code, technical documentation, and project development.

> **Project status:** In active development. The repository is being prepared for the initial application code upload. Features, architecture, and setup instructions will be updated as implementation progresses.

## Product direction

The following describes the current product direction. These capabilities should not be considered shipped until implemented and documented.

- **Expense and income tracking** — record transactions and keep financial activity organized.
- **Categories and budgets** — group transactions and compare spending against planned budgets.
- **Spending insights** — make personal spending patterns easier to review.
- **AI assistant** — provide a conversational interface for finance-related questions, with AI activation configured by the user.
- **User-configured AI provider** — allow users to enter an API key before enabling the chatbot, subject to the provider and integration ultimately supported.
- **Thoughtful mobile experience** — prioritize a clean interface, responsive interactions, and efficient everyday workflows.
- **Privacy-conscious design** — handle financial data and credentials with care.

## Architecture and technology

The final stack will be confirmed after the initial source code is available. The current direction includes:

| Area | Current direction |
| --- | --- |
| Client | Android-focused mobile application |
| Data and backend | Supabase |
| AI | User-configured provider integration |
| Source control | Git and GitHub |

Specific frameworks, supported AI providers, data models, and deployment details will be documented after they are confirmed in the codebase. Avoid relying on this table as a final architecture specification.

## Getting started

### Clone the repository

```bash
git clone https://github.com/prudhvikakkunuri/Spendo-AI.git
cd Spendo-AI
```

### Development setup

The application source and dependency manifests have not yet been documented here. Setup instructions will be added once the initial code is pushed, including the required toolchain, configuration, and run/build commands.

## Configuration and secrets

Before running the application, configure any required service credentials using the documented local development process.

- Never commit API keys, access tokens, passwords, private signing keys, or database credentials.
- Keep local secret/configuration files out of version control and provide a sanitized `.env.example` or equivalent template when needed.
- Do not include Supabase service-role keys in a mobile client. Use appropriate server-side boundaries for privileged operations.
- Configure Supabase Row Level Security (RLS) and least-privilege access policies before using real personal financial data.
- Treat user-provided AI credentials as sensitive. Use platform-appropriate secure storage and avoid logging keys or sending them to unrelated services.
- If a credential is exposed, revoke or rotate it immediately. Removing it in a later commit does not make the exposed credential safe.

## Repository conventions

To keep the project maintainable as it grows:

- Prefer focused, descriptive commits.
- Keep documentation updated alongside changes to setup, features, or architecture.
- Do not commit generated build output, local IDE state, personal data, or secrets.
- Use issues to track bugs and scoped improvements.
- Review changes before merging them into the main development branch.

## Roadmap

This roadmap is directional and will evolve with implementation.

- [ ] Add the initial application source code and dependency manifests.
- [ ] Document the development environment and local run instructions.
- [ ] Implement transaction entry and expense/income history.
- [ ] Add categories, budgets, and spending summaries.
- [ ] Integrate the AI assistant and its user-controlled configuration flow.
- [ ] Define secure persistence and Supabase access policies.
- [ ] Add automated checks and core tests.
- [ ] Document release, troubleshooting, and contribution workflows.

## Issues and feedback

Please use [GitHub Issues](https://github.com/prudhvikakkunuri/Spendo-AI/issues) to report a reproducible bug or propose an improvement. Include relevant context, expected behavior, actual behavior, and steps to reproduce where applicable. Do not include credentials, account details, or real financial records in issue reports.

## License

No license has been declared yet. Unless and until a license is added, no permission is granted to reuse, modify, or redistribute this project's code beyond rights provided by applicable law.

---

<p align="center">
  <sub>Spendo AI · A personal finance project in development</sub>
</p>
>>>>>>> 1a3dbe06325653f71388423b9490ceddded88deb
