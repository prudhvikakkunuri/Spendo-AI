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

    private val NUMBER_WORDS = mapOf(
        "zero" to 0L, "one" to 1L, "two" to 2L, "three" to 3L, "four" to 4L,
        "five" to 5L, "six" to 6L, "seven" to 7L, "eight" to 8L, "nine" to 9L,
        "ten" to 10L, "eleven" to 11L, "twelve" to 12L, "thirteen" to 13L,
        "fourteen" to 14L, "fifteen" to 15L, "sixteen" to 16L, "seventeen" to 17L,
        "eighteen" to 18L, "nineteen" to 19L, "twenty" to 20L, "thirty" to 30L,
        "forty" to 40L, "fifty" to 50L, "sixty" to 60L, "seventy" to 70L,
        "eighty" to 80L, "ninety" to 90L, "hundred" to 100L, "thousand" to 1000L,
        "lakh" to 100000L, "crore" to 10000000L
    )

    fun parseSpeech(transcript: String): VoiceIntent {
        val clean = transcript.trim()
        if (clean.isBlank()) {
            return VoiceIntent.GeneralChat("I couldn't hear any speech. Try saying \"I spent 250 rupees on lunch.\"")
        }

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

        // 4. Savings Inquiry
        val isSavingsQuery = !lower.contains("savings account") && !lower.contains("saving account") &&
                (lower.contains("did i save") || lower.contains("my savings") || lower.contains("money did i save") || lower.contains("how much did i save") || lower.contains("how much money did i save") || lower.contains("saved this month") || lower.contains("how much saved"))
        if (isSavingsQuery) {
            return VoiceIntent.QuerySavings
        }

        // 5. Habits Analysis
        if (lower.contains("analyze") || lower.contains("habits") || lower.contains("spending analysis") || lower.contains("pattern")) {
            return VoiceIntent.QueryAnalyzeHabits
        }

        // 6. Recent Transactions
        if (lower.contains("recent transaction") || lower.contains("latest transaction") || lower.contains("recent expense") || lower.contains("last transaction") || lower.contains("history")) {
            return VoiceIntent.QueryRecentTransactions
        }

        // 7. Top Category Inquiry
        if (lower.contains("which category") || lower.contains("top category") || lower.contains("expense category") || lower.contains("highest spending") || lower.contains("top spending") || lower.contains("biggest category") || lower.contains("category costs")) {
            return VoiceIntent.QueryTopCategory
        }

        // 8. Biggest Expense Inquiry
        if (lower.contains("biggest expense") || lower.contains("largest expense") || lower.contains("biggest purchase") || lower.contains("maximum expense") || lower.contains("highest expense")) {
            return VoiceIntent.QueryBiggestExpense
        }

        // 9. Merchant-Specific Inquiry: "How much did I spend on Swiggy?"
        val merchantQueryPattern = Pattern.compile("(?i)(?:how\\s+much\\s+(?:did\\s+i\\s+)?spend\\s+(?:on|at)|spend\\s+on|spent\\s+on)\\s+([A-Za-z0-9\\s]{2,25}?)(\\?|\\.|$)", Pattern.CASE_INSENSITIVE)
        val mMatcher = merchantQueryPattern.matcher(clean)
        if (mMatcher.find()) {
            val merchant = mMatcher.group(1)?.trim()
            if (!merchant.isNullOrBlank() && !merchant.equals("today", true) && !merchant.equals("this month", true)) {
                return VoiceIntent.QueryMerchantSpend(merchant.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() })
            }
        }

        // 10. Generic Spending Inquiries: "How much did I spend today?", "Show my expenses this month"
        if (lower.contains("what did i spend") || lower.contains("how much did i spend") || lower.contains("spend today") || lower.contains("expenses this month") || lower.contains("spend this month") || lower.contains("show my expenses") || lower.contains("spent today")) {
            return VoiceIntent.InquireSpending(if (lower.contains("month")) "MONTH" else "TODAY")
        }

        // 11. Budget Inquiries: "How much is left in my budget?", "Remaining budget"
        if (lower.contains("budget left") || lower.contains("how much budget") || lower.contains("daily allowance") || lower.contains("left in my budget") || lower.contains("remaining budget") || lower.contains("in my budget") || lower.contains("remaining in budget")) {
            return VoiceIntent.InquireBudget
        }

        // 12. ATM Cash Withdrawal
        if (lower.contains("withdrew") || lower.contains("atm") || lower.contains("cash withdrawal")) {
            val amountPaise = extractPaiseFromSpeech(lower)
            val bank = matchBank(lower) ?: "Karur Vysya Bank"
            return if (amountPaise != null && amountPaise > 0L) {
                VoiceIntent.AtmWithdrawal(amountPaise, bank)
            } else {
                VoiceIntent.AmbiguousExpense(clean, "Please specify the cash withdrawal amount.")
            }
        }

        // 13. Income Credits: "Received 1500 rupees from freelancing", "Salary 50000 credited"
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

        // 14. Natural Language Expense Entry
        // Detect keywords or financial indicators
        val hasExpenseVerb = lower.contains("spent") || lower.contains("spend") ||
                lower.contains("paid") || lower.contains("pay") ||
                lower.startsWith("add") || lower.contains("bought") ||
                lower.contains("buy") || lower.contains("cost") ||
                lower.contains("fare was") || lower.contains("fare is") ||
                lower.contains("bill was") || lower.contains("bill is")

        val hasExpenseNoun = lower.contains("lunch") || lower.contains("dinner") ||
                lower.contains("petrol") || lower.contains("coffee") ||
                lower.contains("snacks") || lower.contains("snack") ||
                lower.contains("groceries") || lower.contains("grocery") ||
                lower.contains("fare") || lower.contains("auto") ||
                lower.contains("cab") || lower.contains("taxi") ||
                lower.contains("biryani") || lower.contains("tea") ||
                lower.contains("uber") || lower.contains("ola") ||
                lower.contains("metro") || lower.contains("food")

        val extractedPaise = extractPaiseFromSpeech(lower)

        if (hasExpenseVerb || (hasExpenseNoun && extractedPaise != null)) {
            if (extractedPaise == null || extractedPaise <= 0L) {
                return VoiceIntent.AmbiguousExpense(clean, "Could not determine the expense amount. How much did you spend?")
            }

            val bank = matchBank(lower) ?: "Primary Bank"
            val category = classifyCategory(lower)
            val merchant = extractMerchant(clean, lower)
            val occurredAt = if (lower.contains("yesterday")) {
                System.currentTimeMillis() - 86400000L
            } else {
                System.currentTimeMillis()
            }

            return VoiceIntent.AddExpense(
                amountPaise = extractedPaise,
                merchant = merchant,
                category = category,
                bankName = bank,
                occurredAtEpochMs = occurredAt
            )
        }

        return VoiceIntent.GeneralChat(clean)
    }

    fun extractPaiseFromSpeech(text: String): Long? {
        val lower = text.lowercase(Locale.ROOT)

        // 1. Direct numeric regex: ₹250, 250 rupees, 120.50, 499, rs 120, etc.
        val numPattern = Pattern.compile("(?:rs\\.?|inr|₹)?\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)\\s*(?:rupees?|rs\\.?|inr|bucks)?", Pattern.CASE_INSENSITIVE)
        val matcher = numPattern.matcher(lower)
        while (matcher.find()) {
            val numStr = matcher.group(1)?.replace(",", "") ?: continue
            try {
                val parts = numStr.split(".")
                val rupees = parts[0].toLong()
                val paise = if (parts.size > 1) parts[1].padEnd(2, '0').take(2).toLong() else 0L
                val total = (rupees * 100L) + paise
                if (total > 0L) return total
            } catch (_: Exception) {}
        }

        // 2. Natural language word numbers: "two hundred fifty", "ninety", "seventy five", etc.
        val convertedFromWords = parseNumberWords(lower)
        if (convertedFromWords != null && convertedFromWords > 0L) {
            return convertedFromWords * 100L
        }

        return null
    }

    private fun parseNumberWords(text: String): Long? {
        val tokens = text.replace("-", " ")
            .replace("and", " ")
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotBlank() }

        var currentTotal = 0L
        var currentSection = 0L
        var matchedAnyNumber = false

        for (token in tokens) {
            val value = NUMBER_WORDS[token]
            if (value != null) {
                matchedAnyNumber = true
                when (value) {
                    100L -> {
                        currentSection = if (currentSection == 0L) 100L else currentSection * 100L
                    }
                    1000L -> {
                        currentTotal += (if (currentSection == 0L) 1L else currentSection) * 1000L
                        currentSection = 0L
                    }
                    100000L -> {
                        currentTotal += (if (currentSection == 0L) 1L else currentSection) * 100000L
                        currentSection = 0L
                    }
                    else -> {
                        currentSection += value
                    }
                }
            }
        }
        val finalResult = currentTotal + currentSection
        return if (matchedAnyNumber && finalResult > 0L) finalResult else null
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
            lower.contains("lunch") || lower.contains("dinner") || lower.contains("breakfast") ||
                    lower.contains("coffee") || lower.contains("biryani") || lower.contains("food") ||
                    lower.contains("burger") || lower.contains("swiggy") || lower.contains("zomato") ||
                    lower.contains("tea") || lower.contains("snacks") || lower.contains("snack") ||
                    lower.contains("restaurant") || lower.contains("cafe") || lower.contains("meal") -> "Food & Drink"

            lower.contains("petrol") || lower.contains("fuel") || lower.contains("diesel") ||
                    lower.contains("uber") || lower.contains("transit") || lower.contains("ola") ||
                    lower.contains("metro") || lower.contains("auto") || lower.contains("cab") ||
                    lower.contains("taxi") || lower.contains("fare") -> "Transit"

            lower.contains("grocery") || lower.contains("groceries") || lower.contains("supermarket") ||
                    lower.contains("blinkit") || lower.contains("zepto") || lower.contains("instamart") ||
                    lower.contains("vegetables") || lower.contains("fruits") || lower.contains("milk") -> "Groceries"

            lower.contains("shopping") || lower.contains("clothes") || lower.contains("shoes") ||
                    lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") -> "Shopping"

            lower.contains("movie") || lower.contains("entertainment") || lower.contains("cinema") ||
                    lower.contains("pvr") || lower.contains("bookmyshow") || lower.contains("ticket") -> "Entertainment"

            lower.contains("subscription") || lower.contains("recharge") || lower.contains("netflix") ||
                    lower.contains("spotify") || lower.contains("airtel") || lower.contains("jio") ||
                    lower.contains("wifi") || lower.contains("electricity") || lower.contains("bill") -> "Bills & Utilities"

            lower.contains("medicine") || lower.contains("pharmacy") || lower.contains("doctor") ||
                    lower.contains("hospital") || lower.contains("clinic") || lower.contains("medical") -> "Health"

            lower.contains("freelance") || lower.contains("salary") -> "Income"
            else -> "Other"
        }
    }

    fun extractMerchant(original: String, lower: String): String {
        // Pattern 1: "bought <item> for <amount>" e.g. "bought coffee for 180"
        val boughtPattern = Pattern.compile("(?i)(?:bought|buy|ordered)\\s+([A-Za-z\\s]{2,25}?)(?:\\s+for|\\s+at|\\s+using|\\s+today|\\s+yesterday|\\.|$)", Pattern.CASE_INSENSITIVE)
        val bMatcher = boughtPattern.matcher(original)
        if (bMatcher.find()) {
            val item = cleanItemName(bMatcher.group(1))
            if (item.isNotBlank()) return item
        }

        // Pattern 2: "<item> fare was <amount>" or "<item> bill was <amount>"
        val farePattern = Pattern.compile("(?i)([A-Za-z\\s]{2,25}?)\\s+(?:fare|bill|ticket|cost)\\s+(?:was|is)", Pattern.CASE_INSENSITIVE)
        val fMatcher = farePattern.matcher(original)
        if (fMatcher.find()) {
            val item = cleanItemName(fMatcher.group(1))
            if (item.isNotBlank()) return item
        }

        // Pattern 3: "spent <amount> on/at/for <item>" or "paid <amount> for/to/on/at <item>"
        val spentOnPattern = Pattern.compile("(?i)(?:spent|paid|spend|pay)\\s+(?:rs\\.?|inr|₹)?\\s*[0-9,]+(?:\\.[0-9]{1,2})?\\s*(?:rupees?|rs\\.?|inr|bucks)?\\s+(?:on|for|at|to)\\s+([A-Za-z\\s]{2,25}?)(?:\\s+using|\\s+through|\\s+via|\\s+with|\\s+yesterday|\\s+today|\\.|$)", Pattern.CASE_INSENSITIVE)
        val sMatcher = spentOnPattern.matcher(original)
        if (sMatcher.find()) {
            val item = cleanItemName(sMatcher.group(1))
            if (item.isNotBlank()) return item
        }

        // Pattern 4: Generic "for/on/at <item>"
        val forPattern = Pattern.compile("(?i)(?:for|on|at)\\s+([A-Za-z\\s]{2,25}?)(?:\\s+paid|\\s+using|\\s+through|\\s+via|\\s+with|\\s+today|\\s+yesterday|\\.|$)", Pattern.CASE_INSENSITIVE)
        val m = forPattern.matcher(original)
        if (m.find()) {
            val item = cleanItemName(m.group(1))
            if (item.isNotBlank()) return item
        }

        // Fallback based on known category words in speech
        return when {
            lower.contains("lunch") -> "Lunch"
            lower.contains("dinner") -> "Dinner"
            lower.contains("breakfast") -> "Breakfast"
            lower.contains("coffee") -> "Coffee"
            lower.contains("tea") -> "Tea"
            lower.contains("snacks") || lower.contains("snack") -> "Snacks"
            lower.contains("petrol") || lower.contains("fuel") -> "Petrol"
            lower.contains("groceries") || lower.contains("grocery") -> "Groceries"
            lower.contains("auto") -> "Auto Fare"
            lower.contains("uber") || lower.contains("ola") || lower.contains("cab") -> "Cab Ride"
            lower.contains("metro") -> "Metro"
            lower.contains("restaurant") -> "Restaurant"
            lower.contains("swiggy") -> "Swiggy"
            lower.contains("zomato") -> "Zomato"
            else -> "Voice Expense"
        }
    }

    private fun cleanItemName(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var cleaned = raw.trim()
        if (cleaned.startsWith("an ", ignoreCase = true)) cleaned = cleaned.substring(3).trim()
        if (cleaned.startsWith("a ", ignoreCase = true)) cleaned = cleaned.substring(2).trim()
        if (cleaned.startsWith("the ", ignoreCase = true)) cleaned = cleaned.substring(4).trim()

        // Filter out unwanted terms
        val lower = cleaned.lowercase(Locale.ROOT)
        if (lower == "today" || lower == "yesterday" || lower.startsWith("rs") || lower.startsWith("rupee")) {
            return ""
        }

        val words = cleaned.split(" ").filter { it.isNotBlank() && !it.equals("yesterday", true) && !it.equals("today", true) }
        return words.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
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
