package com.spendwise.android.util

import com.spendwise.android.domain.model.Transaction
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for exporting transaction ledger records to standard CSV.
 * In accordance with privacy rules, raw notification text and internal hashes are strictly excluded.
 */
object CsvExporter {

    private const val CSV_HEADER = "date,direction,amount_inr,merchant,category,note,source"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

    /**
     * Converts a list of transactions into a standard CSV string.
     */
    fun generateCsv(transactions: List<Transaction>): String {
        val sb = StringBuilder()
        sb.append(CSV_HEADER).append("\n")

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
     * Writes CSV output directly to a stream (e.g. Android document picker Uri stream).
     */
    fun writeCsvToStream(transactions: List<Transaction>, outputStream: OutputStream) {
        val csvContent = generateCsv(transactions)
        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(csvContent)
            writer.flush()
        }
    }

    /**
     * Formats integer paise into standard decimal representation (e.g. 350.00).
     */
    private fun formatAmountForCsv(paise: Long): String {
        val absPaise = Math.abs(paise)
        val rupees = absPaise / 100
        val remainingPaise = absPaise % 100
        return String.format(Locale.ROOT, "%d.%02d", rupees, remainingPaise)
    }

    /**
     * Escapes fields containing commas, double quotes, or newlines per RFC 4180.
     */
    private fun escapeCsvField(field: String): String {
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            val escaped = field.replace("\"", "\"\"")
            return "\"$escaped\""
        }
        return field
    }
}
