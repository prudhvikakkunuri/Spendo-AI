package com.spendwise.android.domain.banking

import java.util.Locale

/**
 * Structured metadata accompanying a transaction in the pending review queue.
 * Encodes bank identification signals, extracted account suffixes, matching status,
 * and parser/matching confidences without requiring breaking database migrations.
 */
data class TransactionReviewMetadata(
    val detectedBank: String? = null,
    val extractedSuffix: String? = null,
    val matchStatus: AccountMatchStatus? = null,
    val matchedAccountId: String? = null,
    val referenceNumber: String? = null,
    val parserConfidence: Float? = null,
    val matchConfidence: Float? = null,
    val explanation: String? = null,
    val cleanNote: String? = null
) {
    fun serialize(): String {
        val b = StringBuilder()
        b.append("[META:")
        detectedBank?.let { b.append("BANK=").append(it.replace(";", ",")).append(";") }
        extractedSuffix?.let { b.append("SUFFIX=").append(it).append(";") }
        matchStatus?.let { b.append("STATUS=").append(it.name).append(";") }
        matchedAccountId?.let { b.append("ACCID=").append(it.replace(";", ",")).append(";") }
        referenceNumber?.let { b.append("REF=").append(it.replace(";", ",")).append(";") }
        parserConfidence?.let { b.append("CONF=").append("%.2f".format(Locale.ENGLISH, it)).append(";") }
        matchConfidence?.let { b.append("MCONF=").append("%.2f".format(Locale.ENGLISH, it)).append(";") }
        b.append("]")
        if (!explanation.isNullOrBlank()) {
            b.append(" ").append(explanation)
        }
        return b.toString().trim()
    }

    companion object {
        fun parse(rawNote: String?): TransactionReviewMetadata {
            if (rawNote.isNullOrBlank()) return TransactionReviewMetadata()
            if (!rawNote.startsWith("[META:")) {
                return TransactionReviewMetadata(cleanNote = rawNote, explanation = rawNote)
            }
            val closeIdx = rawNote.indexOf("]")
            if (closeIdx == -1) {
                return TransactionReviewMetadata(cleanNote = rawNote, explanation = rawNote)
            }

            val metaContent = rawNote.substring(6, closeIdx)
            val trailingExplanation = rawNote.substring(closeIdx + 1).trim().ifBlank { null }

            var bank: String? = null
            var suffix: String? = null
            var status: AccountMatchStatus? = null
            var accId: String? = null
            var ref: String? = null
            var conf: Float? = null
            var mconf: Float? = null

            val parts = metaContent.split(";")
            for (p in parts) {
                val kv = p.split("=")
                if (kv.size == 2) {
                    val k = kv[0].trim()
                    val v = kv[1].trim()
                    when (k) {
                        "BANK" -> bank = v.ifBlank { null }
                        "SUFFIX" -> suffix = v.ifBlank { null }
                        "STATUS" -> status = try { AccountMatchStatus.valueOf(v) } catch (e: Exception) { null }
                        "ACCID" -> accId = v.ifBlank { null }
                        "REF" -> ref = v.ifBlank { null }
                        "CONF" -> conf = v.toFloatOrNull()
                        "MCONF" -> mconf = v.toFloatOrNull()
                    }
                }
            }

            return TransactionReviewMetadata(
                detectedBank = bank,
                extractedSuffix = suffix,
                matchStatus = status,
                matchedAccountId = accId,
                referenceNumber = ref,
                parserConfidence = conf,
                matchConfidence = mconf,
                explanation = trailingExplanation,
                cleanNote = trailingExplanation
            )
        }
    }
}

