# 04 — Notification Parsing Specification

## Inputs and outputs
Input: source package, posted timestamp, title/text/bigText/subText if exposed. Process in memory and pass only minimal fields.

Output is one of:
- `NotTransaction(reasonCode)`
- `Parsed(candidate)`
- `NeedsReview(candidate, reasonCode)`

Candidate fields: amountPaise nullable, currency, direction (DEBIT/CREDIT/UNKNOWN), merchant nullable, timestamp, source package, confidence, review reasons.

## Parsing steps
1. Normalize whitespace and Unicode digits.
2. Look for tested transaction terms: debited, spent, paid, sent, transferred, credited, received, refund, reversed, payment successful.
3. Parse common formats such as `₹1,234.56`, `INR 1,234`, `Rs. 1,234`. These are examples, not universal formats.
4. Determine direction only from explicit evidence.
5. Extract merchant only from reliable known phrases/templates.
6. Score confidence using amount, direction and template match.
7. If amount/direction is missing, multiple amounts are ambiguous, or text may be a balance alert, send to Review or ignore.
8. Deduplicate using multiple signals.

## Initial confidence policy (validate with fixtures)
- >= 0.90: recognized template, one amount and explicit direction.
- 0.65–0.89: partial template or uncertain merchant; Review.
- < 0.65: do not create a confirmed ledger entry.

## Special cases
- Refund/reversal: don't count as ordinary income; review or link to original transaction.
- Failed payment: don't count as a completed debit unless success is explicit.
- Available balance: never mistake it for transaction amount.
- UPI self-transfer: classify as TRANSFER only with reliable evidence.
- Credit-card bill payment: avoid counting both purchase and bill payment as spending; defer complex liability accounting.
- Multiple amounts: use a tested source-specific template or Review.
- Unselected package: ignore.
- No amount: do not fabricate.

## Deduplication
Combine source package, normalized amount, direction, time window, merchant/reference when available, and a local hash of normalized relevant text. Never deduplicate on amount alone because two real purchases may match.

## Synthetic fixture suite
Include debit, credit/salary, amount + balance, decimal/comma formats, duplicate alerts, refund, failed payment, no amount, multiple amounts, mixed-language content, unrelated currency mentions, changed templates and redacted notifications.

## Privacy
No notification payloads to external APIs or LLMs in MVP. Any future remote parsing must be opt-in, disclosed, redacted and have a deterministic fallback.
