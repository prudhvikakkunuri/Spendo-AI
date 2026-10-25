package com.spendwise.android.util

import com.spendwise.android.data.db.entity.TransactionEntity
import com.spendwise.android.domain.model.Transaction
import com.spendwise.android.ui.spendo.model.SpendoTransaction
import com.spendwise.android.ui.spendo.model.TransactionType
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for exporting transaction ledger records to standard CSV.
 * Supports both standard 13-column portable datasets and legacy 7-column export ledger archives.
 */
object CsvExporter {

    private const val CSV_HEADER_LEGACY = "date,direction,amount_inr,merchant,category,note,source"
    const val CSV_HEADER_FULL = "transaction_id,date,time,description,merchant,amount,transaction_type,category,payment_method,bank_account,account_last4,debit_credit,notes"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.ROOT)

    /**
     * Converts a list of domain transactions into a standard 7-column CSV string (legacy compatibility).
     */
    fun generateCsv(transactions: List<Transaction>): String {
        val sb = StringBuilder()
        sb.append(CSV_HEADER_LEGACY).append("\n")

        for (tx in transactions) {
            val dateStr = dateFormat.format(Date(tx.occurredAtEpochMillis))
            val directionStr = tx.direction.name
            val amountStr = formatAmountForCsv(tx.amountPaise)
            val merchantStr = escapeCsvField(tx.merchant)
            val categoryStr = tx.category.name
            val noteStr = escapeCsvField(tx.note ?: "")
            val sourceStr = tx.sourceType.name

            sb.append(dateStr).append(",")
                .append(directionStr).append(",")
                .append(amountStr).append(",")
                .append(merchantStr).append(",")
                .append(categoryStr).append(",")
                .append(noteStr).append(",")
                .append(sourceStr)
                .append("\n")
        }

        return sb.toString()
    }

    /**
     * Writes 7-column CSV output directly to a stream.
     */
    fun writeCsvToStream(transactions: List<Transaction>, outputStream: OutputStream) {
        val csvContent = generateCsv(transactions)
        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(csvContent)
            writer.flush()
        }
    }

    /**
     * Converts a list of TransactionEntity items into standard 13-column CSV.
     */
    fun generateFullCsvFromEntities(transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        sb.append(CSV_HEADER_FULL).append("\n")

        for (tx in transactions) {
            val dateObj = Date(tx.occurredAtEpochMillis)
            val dateStr = dateFormat.format(dateObj)
            val timeStr = timeFormat.format(dateObj)
            val descStr = escapeCsvField(tx.note ?: "${tx.merchant ?: "Transaction"} ${tx.direction}")
            val merchantStr = escapeCsvField(tx.merchant ?: "Unspecified")
            val amountStr = formatAmountForCsv(tx.amountPaise)
            val typeStr = if (tx.direction.equals("CREDIT", ignoreCase = true)) "income" else "expense"
            val categoryStr = escapeCsvField(tx.categoryId)
            val methodStr = if (tx.sourceType == "NOTIFICATION") "UPI" else "Manual"
            val bankStr = escapeCsvField(tx.bankAccountName ?: "Primary Account")
            val last4Str = extractLast4(tx.bankAccountName)
            val debitCreditStr = tx.direction
            val notesStr = escapeCsvField(tx.note ?: "")

            sb.append(tx.id).append(",")
                .append(dateStr).append(",")
                .append(timeStr).append(",")
                .append(descStr).append(",")
                .append(merchantStr).append(",")
                .append(amountStr).append(",")
                .append(typeStr).append(",")
                .append(categoryStr).append(",")
                .append(methodStr).append(",")
                .append(bankStr).append(",")
                .append(last4Str).append(",")
                .append(debitCreditStr).append(",")
                .append(notesStr)
                .append("\n")
        }

        return sb.toString()
    }

    /**
     * Converts a list of UI SpendoTransactions into standard 13-column CSV.
     */
    fun generateFullCsvFromSpendoTransactions(transactions: List<SpendoTransaction>): String {
        val sb = StringBuilder()
        sb.append(CSV_HEADER_FULL).append("\n")

        for (tx in transactions) {
            val dateObj = Date(tx.occurredAtEpochMs)
            val dateStr = dateFormat.format(dateObj)
            val timeStr = timeFormat.format(dateObj)
            val descStr = escapeCsvField(tx.note ?: "${tx.merchant} ${tx.type.name}")
            val merchantStr = escapeCsvField(tx.merchant)
            val amountStr = formatAmountForCsv(tx.amountPaise)
            val typeStr = if (tx.type == TransactionType.INCOME) "income" else "expense"
            val categoryStr = escapeCsvField(tx.category)
            val methodStr = "UPI"
            val bankStr = escapeCsvField(tx.bankAccountName)
            val last4Str = extractLast4(tx.bankAccountName)
            val debitCreditStr = if (tx.type == TransactionType.INCOME) "CREDIT" else "DEBIT"
            val notesStr = escapeCsvField(tx.note ?: "")

            sb.append(tx.id).append(",")
                .append(dateStr).append(",")
                .append(timeStr).append(",")
                .append(descStr).append(",")
                .append(merchantStr).append(",")
                .append(amountStr).append(",")
                .append(typeStr).append(",")
                .append(categoryStr).append(",")
                .append(methodStr).append(",")
                .append(bankStr).append(",")
                .append(last4Str).append(",")
                .append(debitCreditStr).append(",")
                .append(notesStr)
                .append("\n")
        }

        return sb.toString()
    }

    /**
     * Writes full 13-column CSV output directly to a stream.
     */
    fun writeFullEntitiesToStream(transactions: List<TransactionEntity>, outputStream: OutputStream) {
        val csvContent = generateFullCsvFromEntities(transactions)
        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(csvContent)
            writer.flush()
        }
    }

    fun writeSpendoTransactionsToStream(transactions: List<SpendoTransaction>, outputStream: OutputStream) {
        val csvContent = generateFullCsvFromSpendoTransactions(transactions)
        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(csvContent)
            writer.flush()
        }
    }

    private fun extractLast4(accountName: String?): String {
        if (accountName == null) return ""
        val digits = accountName.filter { it.isDigit() }
        return if (digits.length >= 4) digits.takeLast(4) else ""
    }

    /**
     * Formats integer paise into standard decimal representation (e.g. 350.00).
     */
    fun formatAmountForCsv(paise: Long): String {
        val absPaise = Math.abs(paise)
        val rupees = absPaise / 100
        val remainingPaise = absPaise % 100
        return String.format(Locale.ROOT, "%d.%02d", rupees, remainingPaise)
    }

    /**
     * Escapes fields containing commas, double quotes, or newlines per RFC 4180.
     */
    fun escapeCsvField(field: String): String {
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            val escaped = field.replace("\"", "\"\"")
            return "\"$escaped\""
        }
        return field
    }
}
