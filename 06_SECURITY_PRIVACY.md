# 06 — Security, Privacy and Data Handling

## Sensitive data
Notification text may reveal account activity, merchants, amounts and personal relationships. Treat it as financial data even if no credentials are present.

## Rules
1. Explain permission before requesting access.
2. Monitor only user-selected packages.
3. Parse locally; no remote LLM, analytics or financial-data telemetry in MVP.
4. Retain normalized transaction fields only; discard raw notification text after parsing.
5. Offer pause, revoke, export and delete controls.
6. Be honest that notification capture is incomplete and parsing can be wrong.

## Permissions
- Open Notification Access settings only after user action.
- Never attempt to grant access programmatically.
- Re-check permission status when returning to the app.
- Do not add SMS permission in MVP.
- Never use Accessibility Service to scrape other apps.

## Threats and mitigations
- Unlocked-device access: use Android app sandbox; consider vetted encryption at rest based on threat model.
- Log/crash leakage: redact financial values and notification content.
- Unintended package capture: enforce explicit allowlist.
- Malformed alerts: defensive parsing and tests.
- Accidental export: export selected fields and confirm share/save.
- Unintended backup: configure Android backup/data extraction rules deliberately.
- Hidden network transfer: remove unnecessary network permissions and test network behavior.
- Data persistence after deletion: clear ledger, preferences, budgets, commitments, dedupe records and diagnostics.

## Suggested permission copy
“SpendWise can read notifications from the apps you choose to identify possible transactions. Notifications may contain sensitive information. In this version, parsing and transaction storage happen on your device. Some transactions may be missed or misread, so you can review, edit or delete entries. SpendWise does not connect to your bank or initiate payments.”

## Release checklist
- [ ] Verify package filtering and permission state.
- [ ] Verify no notification/ledger data leaves device.
- [ ] Verify no sensitive logs or crash data.
- [ ] Verify backup rules and clear-data behavior.
- [ ] Review privacy policy and Play Data safety declarations against actual behavior.
- [ ] Check current Play policy before adding SMS access or publishing.
- [ ] Provide a user-facing privacy policy before public release.
