# 07 — Test Plan

## Unit tests — parser
- Debit and credit with explicit direction parse correctly.
- Amount plus account balance does not confuse balance with transaction amount.
- Multiple amounts route to Review unless a tested template resolves ambiguity.
- Failed/reversed payments are not confirmed spending.
- Refund is not counted as salary/income.
- Unrelated notification mentioning ₹ is rejected.
- Empty, malformed, oversized and Unicode-heavy text never crashes parser.
- Unselected source package is rejected.

## Unit tests — money/budget
- Reject zero/negative ordinary transaction amounts.
- Paise arithmetic is exact.
- Safe-to-spend displayed amount never goes below zero.
- Zero remaining days handled safely.
- Missing inputs produce “Estimate incomplete”.
- Credits/debits affect totals correctly.
- Transfers excluded from spending if reliably classified.

## Unit tests — dedupe
- Same notification repeated is deduplicated.
- Two similar real purchases remain separate.
- Cross-app potential duplicates are reviewed unless evidence is strong.
- Missing timestamps do not crash deduplication.

## Integration tests
- Listener processes only allowlisted packages.
- Pause toggle stops ingestion.
- Room insert/edit/delete updates dashboard.
- CSV export excludes raw notification body.
- Clear data removes financial records and dedupe state.
- Schema migrations preserve existing records.

## UI/device tests
- Onboarding can be skipped for manual-only use.
- Manual entry works without notification access.
- Permission state refreshes after returning from Settings.
- Review item can be confirmed, edited or ignored.
- Empty states are actionable and accessible.
- Test on at least two physical devices/OEMs: permission enable/disable, reboot, grouped/redacted notifications, battery restrictions, airplane mode, locale/time zone and font scaling.

## Acceptance checklist
- [ ] User understands permission scope.
- [ ] Only selected apps are processed.
- [ ] Ambiguous items are reviewed, not silently confirmed.
- [ ] Estimate is not presented as bank balance.
- [ ] Every detected transaction can be edited/deleted.
- [ ] Core functions work offline.
- [ ] No raw financial notification text in logs.
