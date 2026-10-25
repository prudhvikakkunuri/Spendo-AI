package com.spendwise.android.domain.voice

import java.util.Locale
import java.util.regex.Pattern

sealed class VoiceIntent {
    data class AddExpense(
        val amountPaise: Long,
        val merchant: String,
        val category: String,
        val bankName: String,
        val occurredAtEpochMs: Long = System.currentTimeMillis()
    ) : VoiceIntent()

    data class AddIncome(
        val amountPaise: Long,
        val source: String,
        val category: String,
        val bankName: String
    ) : VoiceIntent()

    data class AtmWithdrawal(
        val amountPaise: Long,
        val bankName: String
    ) : VoiceIntent()

    data class InquireBalance(
        val targetBank: String? = null
    ) : VoiceIntent()

    data class InquireSpending(
        val period: String // "TODAY" or "MONTH"
    ) : VoiceIntent()

    object InquireBudget : VoiceIntent()

    data class QueryMerchantSpend(
        val merchantName: String
    ) : VoiceIntent()

    object QueryTopCategory : VoiceIntent()

    object QueryBiggestExpense : VoiceIntent()

    object QuerySavings : VoiceIntent()

    object QueryAnalyzeHabits : VoiceIntent()

    object QueryRecentTransactions : VoiceIntent()

    object QueryCompareLastMonth : VoiceIntent()

    object QueryReduceSpending : VoiceIntent()

    data class AmbiguousExpense(
        val rawSpeech: String,
        val promptForMissing: String
    ) : VoiceIntent()

    data class GeneralChat(
        val query: String
    ) : VoiceIntent()
}

/**
 * Deterministic natural-language parser for financial commands & queries.
 */
object VoiceFinancialParser {

    fun parseSpeech(transcript: String): VoiceIntent {
        val clean = transcript.trim()
        val lower = clean.lowercase(Locale.ROOT)

        // 1. Balance Inquiries
        if (lower.contains("total balance") || lower.contains("my balance") || lower.contains("account balance")) {
            val bank = matchBank(lower)
            return VoiceIntent.InquireBalance(bank)
        }

        // 2. Spending Inquiries: Compare with last month
        if (lower.contains("compare") && (lower.contains("last month") || lower.contains("previous month") || lower.contains("month"))) {
            return VoiceIntent.QueryCompareLastMonth
        }

        // 3. Spending reduction advice
        if (lower.contains("reduce") || lower.contains("cut cost") || lower.contains("save money") || lower.contains("spend less")) {
            return VoiceIntent.QueryReduceSpending
        }

        // 4. Savings Inquiry: "How much money did I save?", "My savings", "Saved this month"
        val isSavingsQuery = !lower.contains("savings account") && !lower.contains("saving account") &&
                (lower.contains("did i save") || lower.contains("my savings") || lower.contains("money did i save") || lower.contains("how much did i save") || lower.contains("how much money did i save") || lower.contains("saved this month") || lower.contains("how much saved"))
        if (isSavingsQuery) {
            return VoiceIntent.QuerySavings
        }

        // 5. Habits Analysis: "Analyze my spending habits.", "Analyze my spending", "Spending analysis"
        if (lower.contains("analyze") || lower.contains("habits") || lower.contains("spending analysis") || lower.contains("pattern")) {
            return VoiceIntent.QueryAnalyzeHabits
        }

        // 6. Recent Transactions: "Show my recent transactions.", "Latest transactions", "Recent expenses"
        if (lower.contains("recent transaction") || lower.contains("latest transaction") || lower.contains("recent expense") || lower.contains("last transaction") || lower.contains("history")) {
            return VoiceIntent.QueryRecentTransactions
        }

        // 7. Top Category Inquiry: "Which category costs me the most?", "Top category", "Biggest category", "What is my biggest expense category this month?"
        if (lower.contains("which category") || lower.contains("top category") || lower.contains("expense category") || lower.contains("highest spending") || lower.contains("top spending") || lower.contains("biggest category") || lower.contains("category costs")) {
            return VoiceIntent.QueryTopCategory
        }

        // 8. Biggest Expense (Single transaction): "What is my biggest expense?", "Biggest purchase", "Largest expense"
        if (lower.contains("biggest expense") || lower.contains("largest expense") || lower.contains("biggest purchase") || lower.contains("maximum expense") || lower.contains("highest expense")) {
            return VoiceIntent.QueryBiggestExpense
        }

        // 9. Merchant-Specific Inquiry: "How much did I spend on Swiggy?", "Spent on Uber"
        val merchantQueryPattern = Pattern.compile("(?i)(?:how\\s+much\\s+(?:did\\s+i\\s+)?spend\\s+(?:on|at)|spend\\s+on|spent\\s+on)\\s+([A-Za-z0-9\\s]{2,25}?)(\\?|\\.|$)", Pattern.CASE_INSENSITIVE)
        val mMatcher = merchantQueryPattern.matcher(clean)
        if (mMatcher.find()) {
            val merchant = mMatcher.group(1)?.trim()
            if (!merchant.isNullOrBlank() && !merchant.equals("today", true) && !merchant.equals("this month", true)) {
                return VoiceIntent.QueryMerchantSpend(merchant.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() })
            }
        }

        // 10. Generic Spending Inquiries: "How much did I spend today?", "Show my expenses this month", "Spend today"
        if (lower.contains("what did i spend") || lower.contains("how much did i spend") || lower.contains("spend today") || lower.contains("expenses this month") || lower.contains("spend this month") || lower.contains("show my expenses") || lower.contains("spent today")) {
            return VoiceIntent.InquireSpending(if (lower.contains("month")) "MONTH" else "TODAY")
        }

        // 11. Budget Inquiries: "How much is left in my budget?", "Budget left", "Remaining budget"
        if (lower.contains("budget left") || lower.contains("how much budget") || lower.contains("daily allowance") || lower.contains("left in my budget") || lower.contains("remaining budget") || lower.contains("in my budget")) {
            return VoiceIntent.InquireBudget
        }

        // 8. ATM Cash Withdrawal: "I withdrew ₹2,000 from an ATM using KVB"
        if (lower.contains("withdrew") || lower.contains("atm") || lower.contains("cash withdrawal")) {
            val amountPaise = extractPaiseFromSpeech(lower)
            val bank = matchBank(lower) ?: "Karur Vysya Bank"
            return if (amountPaise != null && amountPaise > 0L) {
                VoiceIntent.AtmWithdrawal(amountPaise, bank)
            } else {
                VoiceIntent.AmbiguousExpense(clean, "Please specify the cash withdrawal amount.")
            }
        }

        // 9. Income Credits: "Received 1500 rupees from freelancing", "Salary 50000 credited"
        val isIncomeAction = lower.contains("received") || lower.contains("salary credited") || lower.contains("freelancing") || (lower.contains("credited") && !lower.contains("debit"))
        if (isIncomeAction) {
            val amountPaise = extractPaiseFromSpeech(lower)
            if (amountPaise != null && amountPaise > 0L) {
                val bank = matchBank(lower) ?: "Indian Bank"
                val source = extractIncomeSource(clean, lower)
                val category = if (lower.contains("freelanc")) "Freelance" else if (lower.contains("salary")) "Salary" else "Income"
                return VoiceIntent.AddIncome(
                    amountPaise = amountPaise,
                    source = source,
                    category = category,
                    bankName = bank
                )
            }
        }

        // 10. Natural Language Expense Entry:
        // "I spent 250 rupees on lunch through Kotak today", "Paid 500 for petrol yesterday using HDFC", "Spent ₹280 on biryani using HDFC"
        val isExpenseAction = lower.contains("spent") ||
                lower.contains("paid") ||
                lower.startsWith("add") ||
                lower.contains("bought") ||
                lower.contains("for lunch") ||
                lower.contains("on petrol") ||
                lower.contains("on biryani")

        if (isExpenseAction) {
            val amountPaise = extractPaiseFromSpeech(lower)
            if (amountPaise == null || amountPaise <= 0L) {
                return VoiceIntent.AmbiguousExpense(clean, "Could not determine the expense amount. How much did you spend?")
            }

            val bank = matchBank(lower) ?: "Indian Bank"
            val category = classifyCategory(lower)
            val merchant = extractMerchant(clean, lower)
            val occurredAt = if (lower.contains("yesterday")) {
                System.currentTimeMillis() - 86400000L
            } else {
                System.currentTimeMillis()
            }

            return VoiceIntent.AddExpense(
                amountPaise = amountPaise,
                merchant = merchant,
                category = category,
                bankName = bank,
                occurredAtEpochMs = occurredAt
            )
        }

        return VoiceIntent.GeneralChat(clean)
    }

    fun extractPaiseFromSpeech(lower: String): Long? {
        val pattern = Pattern.compile("(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:rupees?|rs)?")
        val matcher = pattern.matcher(lower)
        while (matcher.find()) {
            val numStr = matcher.group(1)?.replace(",", "") ?: continue
            try {
                val parts = numStr.split(".")
                val rupees = parts[0].toLong()
                val paise = if (parts.size > 1) parts[1].padEnd(2, '0').take(2).toLong() else 0L
                val total = (rupees * 100L) + paise
                if (total > 0L) return total
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun matchBank(lower: String): String? {
        return when {
            lower.contains("kotak") -> "Kotak Mahindra Bank"
            lower.contains("indian bank") || lower.contains("indianbank") -> "Indian Bank"
            lower.contains("kvb") || lower.contains("karur") -> "Karur Vysya Bank"
            lower.contains("hdfc") -> "HDFC Bank"
            lower.contains("icici") -> "ICICI Bank"
            lower.contains("sbi") || lower.contains("yono") -> "State Bank of India"
            lower.contains("axis") -> "Axis Bank"
            else -> null
        }
    }

    fun classifyCategory(lower: String): String {
        return when {
            lower.contains("lunch") || lower.contains("dinner") || lower.contains("coffee") ||
                    lower.contains("biryani") || lower.contains("food") || lower.contains("burger") ||
                    lower.contains("swiggy") || lower.contains("zomato") || lower.contains("tea") -> "Food & Drink"
            lower.contains("petrol") || lower.contains("fuel") || lower.contains("uber") ||
                    lower.contains("transit") || lower.contains("ola") || lower.contains("metro") ||
                    lower.contains("auto") || lower.contains("cab") || lower.contains("taxi") -> "Transit"
            lower.contains("grocery") || lower.contains("groceries") || lower.contains("supermarket") ||
                    lower.contains("blinkit") || lower.contains("zepto") || lower.contains("instamart") -> "Groceries"
            lower.contains("movie") || lower.contains("entertainment") || lower.contains("cinema") ||
                    lower.contains("pvr") || lower.contains("bookmyshow") -> "Entertainment"
            lower.contains("subscription") || lower.contains("recharge") || lower.contains("netflix") ||
                    lower.contains("spotify") || lower.contains("airtel") || lower.contains("jio") -> "Subscriptions"
            lower.contains("freelance") || lower.contains("salary") -> "Income"
            else -> "Other"
        }
    }

    fun extractMerchant(original: String, lower: String): String {
        val forPattern = Pattern.compile("(?i)(?:for|on|at|to)\\s+([A-Za-z0-9\\s]{2,20}?)(?:\\s+paid|\\s+using|\\s+through|\\s+via|\\s+with|\\s+today|\\s+yesterday|\\.|$)", Pattern.CASE_INSENSITIVE)
        val m = forPattern.matcher(original)
        if (m.find()) {
            var found = m.group(1)?.trim() ?: ""
            if (found.startsWith("an ", ignoreCase = true)) {
                found = found.substring(3).trim()
            } else if (found.startsWith("a ", ignoreCase = true)) {
                found = found.substring(2).trim()
            }
            if (found.isNotBlank() && !found.equals("today", true) && !found.equals("yesterday", true)) {
                val words = found.split(" ").filter { it.isNotBlank() }
                return words.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
        }
        return "Voice Expense"
    }

    private fun extractIncomeSource(original: String, lower: String): String {
        val fromPattern = Pattern.compile("(?i)(?:from|by)\\s+([A-Za-z0-9\\s]{2,25}?)(?:\\s+using|\\s+to|\\s+today|\\s+yesterday|\\.|$)", Pattern.CASE_INSENSITIVE)
        val m = fromPattern.matcher(original)
        if (m.find()) {
            val found = m.group(1)?.trim()
            if (!found.isNullOrBlank()) {
                val words = found.split(" ").filter { it.isNotBlank() }
                return words.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
        }
        return if (lower.contains("salary")) "Salary Credit" else "Freelancing"
    }
}
