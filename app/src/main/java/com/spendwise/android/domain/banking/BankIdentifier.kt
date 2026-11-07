package com.spendwise.android.domain.banking

import com.spendwise.android.notification.model.RawNotificationPayload
import java.util.Locale
import java.util.regex.Pattern

data class BankIdentificationResult(
    val bankName: String?,
    val confidence: Float,
    val evidence: List<String>,
    val isAmbiguous: Boolean
)

/**
 * Multi-signal bank identification engine.
 * Prevents false positives (e.g. "Paid to Kotak Securities" vs "Sent from Kotak Bank a/c")
 * using weighted evidence across package name, sender header, structural delimiters, and narration context.
 */
object BankIdentifier {

    data class BankSignature(
        val canonicalName: String,
        val packageNames: Set<String>,
        val senderHeaders: List<String>,
        val originatingPatterns: List<Pattern>,
        val supportContactPatterns: List<Pattern>
    )

    private val supportedSignatures = listOf(
        BankSignature(
            canonicalName = "Karur Vysya Bank",
            packageNames = setOf("com.kvb.dlite", "com.kvb.mobilebanking"),
            senderHeaders = listOf("KVBANK", "KVBDLT", "AD-KVBANK", "VK-KVBANK", "BP-KVBANK", "BZ-KVBANK"),
            originatingPatterns = listOf(
                Pattern.compile("(?i)\\b(?:karur\\s+vysya|kvb)\\b.*\\ba/?c\\b"),
                Pattern.compile("(?i)\\ba/?c\\b.*\\b(?:karur\\s+vysya|kvb)\\b"),
                Pattern.compile("(?i)\\bKVB\\s+ATM\\b")
            ),
            supportContactPatterns = listOf(
                Pattern.compile("(?i)(?:18005721916|18602581916)-KVB")
            )
        ),
        BankSignature(
            canonicalName = "Kotak Mahindra Bank",
            packageNames = setOf("com.msf.kbank.mobile", "com.msf.kopl"),
            senderHeaders = listOf("KOTAK", "KOTAKB", "KMBL", "AD-KOTAKB", "VK-KOTAKB", "BP-KOTAKB", "BZ-KOTAKB"),
            originatingPatterns = listOf(
                Pattern.compile("(?i)\\b(?:from|at)\\s+Kotak\\s+(?:Bank|Mahindra)?\\b"),
                Pattern.compile("(?i)\\bKotak\\s+(?:Bank|Mahindra)?\\s+a/?c\\b"),
                Pattern.compile("(?i)\\bKotak\\s+(?:Bank|Mahindra)?\\s+Credit\\s+Card\\b"),
                Pattern.compile("(?i)\\b(?:debited|paid)\\s+from\\s+Kotak\\b")
            ),
            supportContactPatterns = listOf(
                Pattern.compile("(?i)call\\s+(?:18602662666|18002090000|Kotak)")
            )
        ),
        BankSignature(
            canonicalName = "Indian Bank",
            packageNames = setOf("com.infrasofttech.IndianBank"),
            senderHeaders = listOf("INDBNK", "INDIANB", "AD-INDBNK", "VK-INDBNK", "BP-INDBNK", "BZ-INDBNK"),
            originatingPatterns = listOf(
                Pattern.compile("(?i)^Indian\\s+Bank[:\\s]"),
                Pattern.compile("(?i)\\bIndian\\s+Bank\\s+A/?c\\b"),
                Pattern.compile("(?i)\\ba/?c\\b.*\\bIndian\\s+Bank\\b")
            ),
            supportContactPatterns = listOf(
                Pattern.compile("(?i)call\\s+180042500000")
            )
        ),
        BankSignature(
            canonicalName = "HDFC Bank",
            packageNames = setOf("com.hdfcbank.android", "com.snapwork.hdfc"),
            senderHeaders = listOf("HDFCBK", "HDFC", "AD-HDFCBK", "VK-HDFCBK", "BP-HDFCBK", "BZ-HDFCBK"),
            originatingPatterns = listOf(
                Pattern.compile("(?i)\\bHDFC\\s+Bank\\s+A/?c\\b"),
                Pattern.compile("(?i)\\ba/?c\\b.*\\bHDFC\\s+Bank\\b"),
                Pattern.compile("(?i)\\bfrom\\s+HDFC\\s+Bank\\b"),
                Pattern.compile("(?i)\\bHDFC\\s+Bank\\s+Card\\b")
            ),
            supportContactPatterns = listOf(
                Pattern.compile("(?i)call\\s+1800(?:2026161|1600|2664332)")
            )
        ),
        BankSignature(
            canonicalName = "State Bank of India",
            packageNames = setOf("com.sbi.lotusintouch", "com.sbi.upi"),
            senderHeaders = listOf("SBIUPI", "SBINB", "SBIPSG", "ATMSBI", "AD-SBIUPI", "VK-SBIUPI"),
            originatingPatterns = listOf(
                Pattern.compile("(?i)\\bState\\s+Bank\\s+of\\s+India\\b"),
                Pattern.compile("(?i)\\bSBI\\s+A/?c\\b"),
                Pattern.compile("(?i)\\ba/?c\\b.*\\bSBI\\b")
            ),
            supportContactPatterns = listOf(
                Pattern.compile("(?i)call\\s+1800(?:112211|4253800)")
            )
        ),
        BankSignature(
            canonicalName = "ICICI Bank",
            packageNames = setOf("com.csam.icici.bank.imobile"),
            senderHeaders = listOf("ICICIB", "ICICI", "AD-ICICIB", "VK-ICICIB", "BP-ICICIB"),
            originatingPatterns = listOf(
                Pattern.compile("(?i)\\bICICI\\s+Bank\\s+A/?c\\b"),
                Pattern.compile("(?i)\\ba/?c\\b.*\\bICICI\\s+Bank\\b"),
                Pattern.compile("(?i)\\bICICI\\s+Bank\\s+Credit\\s+Card\\b")
            ),
            supportContactPatterns = listOf(
                Pattern.compile("(?i)call\\s+18001080")
            )
        ),
        BankSignature(
            canonicalName = "Axis Bank",
            packageNames = setOf("com.axis.mobile"),
            senderHeaders = listOf("AXISBK", "AXIS", "AD-AXISBK", "VK-AXISBK", "BP-AXISBK"),
            originatingPatterns = listOf(
                Pattern.compile("(?i)\\bAxis\\s+Bank\\s+A/?c\\b"),
                Pattern.compile("(?i)\\ba/?c\\b.*\\bAxis\\s+Bank\\b"),
                Pattern.compile("(?i)\\bAxis\\s+Bank\\s+Card\\b")
            ),
            supportContactPatterns = listOf(
                Pattern.compile("(?i)call\\s+1860(?:4195555|5005555)")
            )
        )
    )

    /**
     * Identifies the originating bank from a notification payload.
     * Guards against false attribution when a bank name appears only in the merchant or recipient field.
     */
    fun identifyBank(payload: RawNotificationPayload): BankIdentificationResult {
        val title = payload.title.orEmpty().trim()
        val text = payload.text.orEmpty().trim()
        val bigText = payload.bigText.orEmpty().trim()
        val combinedBody = "$text $bigText".trim()
        val fullCombined = "$title $combinedBody".trim()

        val results = mutableListOf<Pair<BankSignature, Float>>()
        val candidateEvidence = mutableMapOf<String, MutableList<String>>()

        // Payee/merchant negation: detect if bank keyword appears exclusively after 'to' or 'towards'
        val merchantPattern = Pattern.compile("(?i)(?:to|towards|paid\\s+to|transferred\\s+to)\\s+([A-Za-z0-9\\s.'&-]{2,50}?)")
        val merchantMatch = merchantPattern.matcher(combinedBody)
        val merchantPart = if (merchantMatch.find()) merchantMatch.group(1)?.uppercase(Locale.ROOT) ?: "" else ""

        for (sig in supportedSignatures) {
            var score = 0f
            val evidenceList = mutableListOf<String>()

            // 1. Direct Package Name Match (Strongest signal: 0.55)
            if (sig.packageNames.contains(payload.packageName)) {
                score += 0.55f
                evidenceList.add("Package match: ${payload.packageName}")
            }

            // 2. Sender / Title Header Match (0.40)
            val upperTitle = title.uppercase(Locale.ROOT)
            val headerMatch = sig.senderHeaders.any { header ->
                upperTitle.contains(header) || upperTitle == header
            }
            if (headerMatch) {
                score += 0.40f
                evidenceList.add("Sender header match in title: $title")
            }

            // 3. Originating account pattern in body (0.35)
            for (pat in sig.originatingPatterns) {
                if (pat.matcher(fullCombined).find()) {
                    score += 0.35f
                    evidenceList.add("Originating account pattern match")
                    break
                }
            }

            // 4. Bank official helpline pattern (e.g. 18005721916-KVB) (0.30)
            for (pat in sig.supportContactPatterns) {
                if (pat.matcher(fullCombined).find()) {
                    score += 0.30f
                    evidenceList.add("Official bank support signature match")
                    break
                }
            }

            // 5. False positive penalty: Bank name only in merchant narration
            val upperBankShort = sig.canonicalName.split(" ").first().uppercase(Locale.ROOT)
            val inMerchantOnly = merchantPart.contains(upperBankShort) && !sig.packageNames.contains(payload.packageName) && !headerMatch
            if (inMerchantOnly && !sig.supportContactPatterns.any { it.matcher(fullCombined).find() }) {
                score -= 0.40f
                evidenceList.add("Penalized: Keyword appeared in merchant/payee position")
            }

            if (score > 0.30f) {
                results.add(sig to score.coerceIn(0f, 1f))
                candidateEvidence[sig.canonicalName] = evidenceList
            }
        }

        if (results.isEmpty()) {
            return BankIdentificationResult(
                bankName = null,
                confidence = 0f,
                evidence = listOf("No recognized bank signature matched"),
                isAmbiguous = false
            )
        }

        // Sort descending by score
        val sorted = results.sortedByDescending { it.second }
        val best = sorted.first()

        // Check for competing bank scores (ambiguity)
        val isAmbiguous = sorted.size > 1 && (best.second - sorted[1].second < 0.20f)

        return BankIdentificationResult(
            bankName = if (!isAmbiguous && best.second >= 0.50f) best.first.canonicalName else if (best.second >= 0.40f) best.first.canonicalName else null,
            confidence = best.second,
            evidence = candidateEvidence[best.first.canonicalName] ?: emptyList(),
            isAmbiguous = isAmbiguous
        )
    }
}
