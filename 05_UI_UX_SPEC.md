# 05 — UI/UX Specification

## Visual direction
Trustworthy, minimal personal finance UI. Dark mode first with deep charcoal surfaces and restrained lime/emerald accent; keep contrast accessible. Material 3, readable typography, scalable text, accessible labels and generous touch targets. Use Indian currency formatting. Avoid shame-based messaging.

## Navigation
Bottom navigation: Home, Transactions, Review, Settings. Budget setup is accessible from a dashboard card and settings.

## Screens
1. **Onboarding:** “Track spending without linking your bank.” Explain notification scope, local processing and limitations. Allow manual-only mode.
2. **Notification access:** Explain why it is needed; button opens Android settings. Show Enabled/Not enabled and how to revoke access.
3. **Choose apps:** Show relevant installed apps; require explicit per-app toggle; pause all detection.
4. **Home:** spent today, received today, safe-to-spend estimate, month-to-date spending, categories, recent transactions, detection status and review count.
5. **Transactions:** date-grouped list; merchant/description, category, time and signed amount; filters by date/category/direction/source; edit and delete.
6. **Review:** extracted amount/direction/source and reason for uncertainty; Confirm, Edit, Ignore. Do not persist raw notification text by default.
7. **Budget setup:** optional income, starting spendable balance, savings reserve, commitments and due dates. Explain formula and “Estimate incomplete” state.
8. **Settings:** permission status, monitored apps, pause/resume, CSV export, theme, privacy summary, clear local data and version.

## Interaction rules
- Manual tracking must work without notification access.
- Amount must be positive; direction is a separate field.
- Use undo or confirmation for destructive actions.
- Uncertain transactions are excluded from confirmed totals until reviewed.
- Provide actionable empty/error states.
- No fake production data; label demo data.
- Never describe the estimate as a verified bank balance.
