# 02 — System Design

## Recommended stack
- Kotlin, Jetpack Compose, Material 3
- ViewModel + StateFlow, Coroutines/Flow
- Room (SQLite), DataStore
- Navigation Compose
- JUnit and Compose UI tests
- Min SDK: decide after confirming target devices; Android 10/API 29+ is an initial option, not a locked requirement. Use current stable compile/target SDK supported by the build environment.

## Architecture
```text
Android notification
  -> NotificationListenerService
  -> selected-package allowlist + filters
  -> pure Kotlin TransactionParser
  -> validation + confidence score
  -> deduplication
  -> Room local database
  -> Repository/Flow -> ViewModels -> Compose UI
                              Dashboard / Transactions / Review / Budget / Settings
```

## Suggested package structure
```text
data/db/                 AppDatabase, entities, DAOs
data/preferences/        UserPreferences
data/repository/         TransactionRepository, BudgetRepository
domain/model/            Transaction, TransactionCandidate
domain/parser/           TransactionParser, ParserResult, MerchantNormalizer
domain/budget/            SafeToSpendCalculator
domain/dedup/             TransactionDeduplicator
notification/             TransactionNotificationListener, NotificationAccessManager
ui/navigation/
ui/onboarding/
ui/dashboard/
ui/transactions/
ui/review/
ui/budget/
ui/settings/
util/                     CsvExporter, ClockProvider
```

## Responsibilities
- `TransactionNotificationListener`: receive posted notifications; do minimal work in callback; extract exposed fields and dispatch parsing off the UI thread.
- `NotificationAccessManager`: report access state and open system settings only after a user action.
- `TransactionParser`: deterministic, testable parsing; returns Parsed, NotTransaction or NeedsReview.
- `TransactionDeduplicator`: combine package, amount, direction, time window, merchant/reference and local fingerprint; never use amount alone.
- `TransactionRepository`: single source of truth for writes and reads.
- `SafeToSpendCalculator`: pure function with unit tests.
- Room: persist normalized ledger, budgets, commitments and settings.
- ViewModels: expose state using StateFlow; keep business logic out of composables.
- CSV exporter: export selected fields using Android's document picker/share sheet.

## Data flow
1. Receive notification.
2. Check monitoring switch and selected package allowlist.
3. Extract title/text/bigText only if exposed by Android.
4. Parse locally; validate amount and direction.
5. Check for duplicates.
6. Save high-confidence candidates or route uncertain ones to Review.
7. Update UI through Room Flow.
8. Handle failures without crashing; never log raw text or financial values.

## Android constraints
- Notification listener access requires user approval in system settings; app cannot grant itself access.
- Notifications can be hidden, grouped, delayed, redacted, or unavailable.
- Some apps expose generic text only; never promise 100% capture.
- Test physical devices and OEM battery behavior.
- Do not use Accessibility Service to scrape other apps.
- Avoid SMS permission in MVP; review current Google Play policy before considering it.

## Security and non-functional requirements
- Local-first; no network needed for core features.
- Filter only selected packages.
- Do not send notification text or financial data to servers, LLMs, analytics or crash logs.
- Configure backup rules deliberately.
- Use vetted encryption if needed; never invent custom cryptography.
- Use migrations; never destroy user data on upgrade.
- Accessible contrast, scalable text and adequate touch targets.
- Parser changes must include fixture tests.
