# Bank Notification & SMS Fixtures Reference Document

## 1. Regulatory Context & Framework
Under Reserve Bank of India (RBI) mandates (**RBI/2017-18/15 DBR.No.Leg.BC.78/09.07.005/2017-18** - *Customer Protection – Limiting Liability of Customers in Unauthorised Electronic Banking Transactions*), all scheduled commercial banks in India are required to register customers for mandatory SMS alerts and, wherever available, email alerts for electronic banking transactions.

---

## 2. Bank Fixture Classification & Verification Register

| Bank | Classification | Official Reference / Evidence Source | Verified Sender Headers | Masking & Format Notes |
|---|---|---|---|---|
| **Karur Vysya Bank (KVB)** | **User-Provided Live Fixture** & Officially Documented | Official KVB DLite notification guidelines & live user transaction alert ([KVB Official Site](https://www.kvb.co.in/personal/digital-banking/sms-banking/)) | `KVBANK`, `KVBDLT`, `AD-KVBANK` | 16/12-digit masked `XXXXXXXXXXXX9362`, UTR delimiter `info :P2A/...`, Bal delimiter `Avl Bal INR ...`, Support contact `18005721916-KVB` |
| **Kotak Mahindra Bank** | **Officially Documented** & Synthetic Fixtures | Kotak 811 Alert Services & SMS Banking Circulars ([Kotak Official Site](https://www.kotak.com/en/customer-service/alert-services.html)) | `KOTAK`, `KOTAKB`, `KMBL` | Masked as `a/c ending 1234`, `A/c *1234`, `Card ending 4821`. Bal prefix `Avl Bal INR` or `Bal:` |
| **Indian Bank** | **Officially Documented** & Synthetic Fixtures | Indian Bank IndOASIS Alert Specifications ([Indian Bank Official Site](https://www.indianbank.in/departments/digital-banking/)) | `INDBNK`, `INDIANB` | Masked as `A/c *5678` or `A/c ...5678`. Delimiters `debited by Rs.` / `Clear Bal: Rs.` |
| **HDFC Bank** | **Officially Documented** | HDFC Bank InstaAlerts Specification ([HDFC Bank Official Site](https://www.hdfcbank.com/personal/ways-to-bank/online-banking/insta-alerts)) | `HDFCBK`, `HDFC` | Masked as `a/c **4321` or `Card ending 1122`. Delimiters `debited from`, `Avl bal: Rs` |
| **State Bank of India (SBI)** | **Officially Documented** | SBI Quick - SMS & Mobile Banking Specifications ([SBI Official Site](https://sbi.bank.in/web/personal-banking/digital/mobile-banking/sbi-quick)) | `SBIUPI`, `SBINB`, `SBIPSG` | Masked as `A/C ending 9876`. Delimiters `debited by Rs.`, `transfer to`, `Ref No`, `Avl Bal` |
| **ICICI Bank** | **Officially Documented** | ICICI Bank Alerts Service ([ICICI Bank Official Site](https://www.icicibank.com/personal-banking/digital-banking/alerts)) | `ICICIB`, `ICICI` | Masked as `Acct XX5544`. Delimiters `debited with INR`, `Available Balance is INR` |
| **Axis Bank** | **Officially Documented** | Axis Bank SMS Banking Guidelines ([Axis Bank Official Site](https://www.axisbank.com/retail/digital-banking/sms-banking)) | `AXISBK`, `AXIS` | Masked as `A/c no. XX3322`. Delimiters `debited from`, `Avl Bal: INR` |

---

## 3. Fixture Categories

### Category A: Officially Documented / User Verified
1. **KVB Live User Debit**:
   `"Your a/c XXXXXXXXXXXX9362 is debited Rs. 50.00 on 24-Oct-2026 to BHAVAPRITA STATIONERY AND EMBROIDERY WORK info :P2A/291530634673. Avl Bal INR 59.2 Not You? call 18005721916-KVB"`
   - Extraction: Amount = 5000 paise (₹50.00), Bal = 5920 paise (₹59.20), Payee = "BHAVAPRITA STATIONERY AND EMBROIDERY WORK", Suffix = 9362, Ref = P2A/291530634673.

2. **Kotak Live / Official Debit**:
   `"Sent Rs. 350.00 from Kotak Bank a/c ending 1234 to ZOMATO info :P2A/112233445566. Avl Bal INR 1250.00 Not you? Call 18002090000"`
   - Extraction: Amount = 35000 paise (₹350.00), Bal = 125000 paise (₹1,250.00), Payee = "ZOMATO", Suffix = 1234.

3. **Indian Bank Official Debit**:
   `"Indian Bank: A/c *5678 debited by Rs.120.00 on 24-Oct-2026 by UPI to CHAI POINT info: 432109876543. Avl Bal Rs.3450.00"`
   - Extraction: Amount = 12000 paise (₹120.00), Bal = 345000 paise (₹3,450.00), Payee = "CHAI POINT", Suffix = 5678.

### Category B: Synthetic Test Fixtures (Boundary & Resilience Verification)
These fixtures verify structural edge cases (missing suffix, decimal formatting, punctuation, ATM cash movements, refunds, reversals):
- **ATM Cash Withdrawal**: `"Cash withdrawal of Rs. 2,000.00 at KVB ATM. Avl Bal INR 3,059.20-KVB"` $\rightarrow$ Marked as `isAtmCashMovement = true`, category `TRANSFER` to prevent double-counting with cash spend.
- **Refund / Reversal**: `"Refund of Rs. 350.00 credited to Kotak Bank a/c ending 1234 from ZOMATO. Bal: INR 1600.00"` $\rightarrow$ Marked as `CREDIT`, category `REFUND`.
- **False Attribution Trap**: `"Paid Rs. 500 to Kotak Securities via PhonePe from HDFC Bank a/c **4321"` $\rightarrow$ Identified originating bank = HDFC Bank, payee = Kotak Securities (prevents false attribution to Kotak).
- **Balance Alert (Non-transaction)**: `"Your clear balance for a/c ending 1234 is INR 15,200.00"` $\rightarrow$ Ignored as transaction, updates balance snapshot only.
- **Security / OTP (Non-transaction)**: `"Your OTP is 948201 for login. Do not share with anyone."` $\rightarrow$ Discarded immediately.
- **Promotional (Non-transaction)**: `"Get pre-approved personal loan of Rs 5 Lakhs instantly. Apply now!"` $\rightarrow$ Discarded immediately.

