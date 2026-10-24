# 10 — Decisions and Open Questions

## Initial decisions
| Topic | MVP decision | Why |
|---|---|---|
| Platform | Android first | Notification access is central |
| Stack | Kotlin + Compose | Native Android APIs |
| Storage | Local Room DB | Offline-first and privacy |
| Parsing | Deterministic rules | Predictable, testable, no AI data transfer |
| Source | User-selected notification packages | Least privilege |
| Manual entry | Always available | Notifications can be missed |
| SMS | Deferred | Additional policy/permission considerations |
| Bank integration | Out of scope | Product requirement |
| External LLM | Not in MVP | Sensitive financial data stays local |

## Validate before release
1. Minimum supported Android version and actual target devices.
2. Which specific payment/banking apps to support first; test templates rather than assuming.
3. Whether high-confidence entries auto-confirm or all require confirmation at first.
4. Exact budget period and formula users understand.
5. Whether encryption at rest is needed for the chosen threat model.
6. Backup policy and encrypted local export requirements.
7. Privacy policy and current Play Store disclosures.
8. Final app name and application ID.
9. Whether product remains free/offline or optional sync is ever considered.

## Risks
- Notification formats differ and change.
- Some transactions produce no readable notification.
- One transaction can generate multiple alerts.
- Refunds, transfers, credit-card bill payments and reversals distort totals if misclassified.
- Notification access may concern users; explain it clearly.
- Play policy changes; verify before release.
- Safe-to-spend accuracy depends on entered starting balance, captured transactions and commitments.

## MVP success measures
Time to add a manual transaction, parser fixture accuracy, false-positive rate on labeled synthetic data, duplicate rate, review resolution rate, user understanding of permissions and estimate limitations.
