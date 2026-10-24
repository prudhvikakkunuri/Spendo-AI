# 01 — Product Requirements Document (PRD)

## Product overview
SpendWise helps people track daily spending without linking bank accounts. With user-authorized Android notification access, it detects likely transaction alerts from selected apps, extracts available details, and creates a proposed ledger entry. Users can review/correct entries, see categories and calculate an estimated daily safe-to-spend amount.

## Problem
Manual logging is tedious. Bank linking can feel invasive. Transaction notifications are convenient but vary in wording and may omit merchant details.

## Goals
- Reduce effort required to log expenses.
- Make transaction capture transparent and user-controlled.
- Provide understandable spending summaries and daily budget estimates.
- Keep financial records local by default.
- Remain useful when parsing fails.

## MVP non-goals
No payments/transfers, bank APIs, open banking, SMS reading, cloud sync, multi-device sync, lending/investment advice, receipt OCR, or external LLM processing.

## Target user
An Android user who wants low-effort expense tracking, prefers not to link a bank account, and is willing to grant notification access after reviewing a privacy explanation.

## Core journey
1. Install and open app.
2. Read permission/privacy explanation.
3. Optionally enable Notification Access in Android Settings.
4. Select supported apps to monitor.
5. A transaction notification arrives.
6. Parser extracts amount, direction, timestamp and merchant/reference if available.
7. Deduplication checks for repeats.
8. High-confidence entries are saved; uncertain entries go to Review.
9. User can edit, categorize, confirm, ignore or delete.
10. Dashboard updates from the local ledger.

## P0 features
- Onboarding and permission explanation.
- Notification access setup and status.
- User-selected monitored-app allowlist and pause toggle.
- Debit/credit candidate detection.
- Local Room database.
- Review queue for uncertain parses.
- Manual add/edit/delete.
- Categories: Food, Transport, Shopping, Bills, Health, Entertainment, Income, Transfers, Other.
- Dashboard: spent today, income today, month-to-date spend, category summary and safe-to-spend estimate.
- Transaction history and filters.
- Duplicate detection.
- CSV export.
- Settings: monitored apps, permission status, export and clear local data.

## P1 later
Custom categories and merchant rules, recurring transaction hints, budget goals, optional local backup/restore, widgets and configurable reminders.

## Functional requirements
- FR-01 Explain notification access before opening settings.
- FR-02 Allow detection to be paused or disabled.
- FR-03 Monitor only explicitly selected supported packages.
- FR-04 Ignore unselected packages.
- FR-05 Create a candidate only when notification text supports a likely transaction.
- FR-06 Capture amount, direction, timestamp, source package, merchant if available, currency and review status.
- FR-07 Never invent missing or ambiguous fields.
- FR-08 Route low-confidence candidates to Review.
- FR-09 Allow manual create/edit/categorize/delete.
- FR-10 Core functionality works offline.
- FR-11 Deduplicate repeated alerts using multiple signals, never amount alone.
- FR-12 Recalculate dashboard from local data.
- FR-13 Explain the safe-to-spend formula.
- FR-14 Export selected ledger fields to CSV.
- FR-15 Allow clearing all local data.
- FR-16 Never initiate or modify a payment.
- FR-17 Do not transmit financial records or notification text off-device in MVP.

## Safe-to-spend estimate
Use deterministic code. Suggested formula:
- `available_funds` = optional user-entered starting spendable balance + confirmed credits − confirmed debits during the budget period.
- `flexible_budget` = max(0, available_funds − remaining commitments − savings reserve).
- `daily_safe_to_spend` = flexible_budget / max(1, remaining days in period).

If starting balance or commitments are missing, show “Estimate incomplete.” This estimate is not a verified bank balance. Transfers and refunds need careful treatment and should be reviewed when uncertain.

## Acceptance criteria
- Manual tracking works without notification permission.
- App remains usable if permission is denied or later revoked.
- Only selected packages are processed.
- Duplicate alerts do not create duplicate entries in tested cases.
- Uncertain transactions appear in Review.
- Data persists across restarts and core features work offline.
- CSV export excludes raw notification body and account identifiers.
- Clear-data removes ledger, budget settings, dedupe fingerprints and parser diagnostics.
