package com.spendwise.android.util

import com.spendwise.android.data.db.entity.TransactionEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Utility for parsing and importing transactions from RFC-4180 compliant CSV files.
 * Supports both Spendo 13-column synthetic/portable CSV datasets and 7-column export ledger archives.
 */
object CsvImporter {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

    /**
     * Parses an input stream containing CSV content and produces a list of normalized TransactionEntity items.
     */
    fun parseCsv(inputStream: InputStream): List<TransactionEntity> {
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val lines = mutableListOf<String>()
        var line: String? = reader.readLine()
        while (line != null) {
            lines.add(line)
            line = reader.readLine()
        }

        if (lines.isEmpty()) return emptyList()

        val headerRow = parseCsvLine(lines.first())
        val headerMap = headerRow.mapIndexed { index, name -> name.trim().lowercase(Locale.ROOT) to index }.toMap()

        val results = mutableListOf<TransactionEntity>()

        for (i in 1 until lines.size) {
            val rawLine = lines[i]
            if (rawLine.isBlank()) continue

            val tokens = parseCsvLine(rawLine)
            if (tokens.isEmpty()) continue

            try {
                val entity = mapRowToEntity(tokens, headerMap)
                if (entity != null) {
                    results.add(entity)
                }
            } catch (_: Exception) {
                // Ignore malformed individual rows to allow best-effort batch recovery
            }
        }

        return results
    }

    private fun mapRowToEntity(tokens: List<String>, headerMap: Map<String, Int>): TransactionEntity? {
        fun get(col: String): String? {
            val idx = headerMap[col] ?: return null
            return if (idx < tokens.size) tokens[idx].trim() else null
        }

        // 1. Amount parsing (converts decimal INR to integer paise)
        val amountStr = get("amount") ?: get("amount_inr") ?: return null
        val cleanAmount = amountStr.replace(",", "").replace("₹", "").trim()
        val amountDouble = cleanAmount.toDoubleOrNull() ?: return null
        val amountPaise = Math.round(amountDouble * 100.0)
        if (amountPaise <= 0L) return null

        // 2. Date / Time parsing
        val dateStr = get("date") ?: return null
        val timeStr = get("time")
        val timestamp = parseTimestamp(dateStr, timeStr)

        // 3. Direction and Type
        val rawDirection = get("debit_credit") ?: get("direction") ?: "DEBIT"
        val rawType = get("transaction_type") ?: ""
        val direction = when {
            rawDirection.equals("CREDIT", ignoreCase = true) || rawType.equals("income", ignoreCase = true) || rawType.equals("refund", ignoreCase = true) -> "CREDIT"
            else -> "DEBIT"
        }

        // 4. Identifiers & Metadata
        val id = get("transaction_id") ?: "imp_${timestamp}_${Math.abs(amountPaise)}"
        val merchant = get("merchant") ?: "Unspecified Merchant"
        val category = get("category") ?: "Other"
        val bankAccount = get("bank_account") ?: "Primary Account"
        val note = get("notes") ?: get("note") ?: get("description")

        return TransactionEntity(
            id = id,
            amountPaise = amountPaise,
            currencyCode = "INR",
            direction = direction,
            merchant = merchant,
            note = note,
            categoryId = category,
            occurredAtEpochMillis = timestamp,
            sourceType = "CSV_IMPORT",
            sourcePackage = "com.spendwise.android.csv",
            confidence = 1.0f,
            reviewStatus = "CONFIRMED",
            bankAccountName = bankAccount
        )
    }

    private fun parseTimestamp(dateStr: String, timeStr: String?): Long {
        if (!timeStr.isNullOrBlank()) {
            try {
                val full = "$dateStr $timeStr"
                val parsed = dateTimeFormat.parse(full)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        try {
            val parsed = dateFormat.parse(dateStr)
            if (parsed != null) return parsed.time
        } catch (_: Exception) {}
        return System.currentTimeMillis()
    }

    /**
     * RFC-4180 compliant CSV line tokenizer handling commas within quotation marks and doubled quotes.
     */
    fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString())
        return tokens
    }
}
