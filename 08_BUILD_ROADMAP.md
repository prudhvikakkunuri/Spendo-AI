# 08 — Build Roadmap

## Phase 0: Skeleton
Create native Kotlin/Compose app, theme, navigation, README and test setup. Build and launch before notification work.
**Done when:** app compiles and placeholder screens navigate.

## Phase 1: Manual-first ledger
Room entities/DAOs/repository, add/edit/delete, categories, history, dashboard totals and CSV export.
**Done when:** manual tracking works offline and survives restart.

## Phase 2: Budget logic
Budget settings, commitments and pure `SafeToSpendCalculator` with tests. Explain formula and incomplete inputs.
**Done when:** calculations are tested and UI labels them as estimates.

## Phase 3: Notification access
Permission explanation, settings deep-link, `NotificationListenerService`, package allowlist and pause toggle.
**Done when:** ingestion occurs only when access is enabled, monitoring is on and package is selected.

## Phase 4: Parser and review
Rule-based parser, synthetic fixtures, confidence/review reasons, dedupe and review screen.
**Done when:** fixture tests pass and uncertain candidates aren't silently confirmed.

## Phase 5: Privacy hardening
Verify no financial network traffic, review backup rules/exported components, implement clear-data and test malformed input.
**Done when:** privacy checklist passes.

## Phase 6: Beta
Test with synthetic data first; opt-in usability testing; measure parser false positives and duplicate rate without collecting financial content. Fix before adding source apps.

## Vibecoding workflow
Implement one phase at a time. After each phase, list changed files, run build/tests, report actual results and known limitations. Do not add backend, analytics, SMS permissions or LLM API without explicit approval.
