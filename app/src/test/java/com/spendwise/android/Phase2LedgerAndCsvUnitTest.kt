package com.spendwise.android

import com.spendwise.android.data.db.entity.TransactionEntity
import com.spendwise.android.domain.model.ReviewStatus
import com.spendwise.android.domain.model.SourceType
import com.spendwise.android.domain.model.Transaction
import com.spendwise.android.domain.model.TransactionCategory
import com.spendwise.android.domain.model.TransactionDirection
import com.spendwise.android.domain.model.toDomain
import com.spendwise.android.domain.model.toEntity
import com.spendwise.android.util.CsvExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * Unit tests verifying Phase 2 Room data mappings, positive paise invariants, and CSV export.
 */
class Phase2LedgerAndCsvUnitTest {

    @Test
    fun transactionModel_rejectsZeroOrNegativePaise() {
        try {
            Transaction(
                id = "tx_invalid_0",
                amountPaise = 0L,
                direction = TransactionDirection.DEBIT,
                merchant = "Test Payee",
                category = TransactionCategory.OTHER,
                occurredAtEpochMillis = 1000L,
                sourceType = SourceType.MANUAL
            )
            fail("Expected IllegalArgumentException for zero paise")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("greater than 0") == true)
        }

        try {
            Transaction(
                id = "tx_invalid_neg",
                amountPaise = -5000L,
                direction = TransactionDirection.DEBIT,
                merchant = "Test Payee",
                category = TransactionCategory.OTHER,
                occurredAtEpochMillis = 1000L,
                sourceType = SourceType.MANUAL
            )
            fail("Expected IllegalArgumentException for negative paise")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("greater than 0") == true)
        }
    }

    @Test
    fun entityToDomainMapping_preservesAllFieldsAndEnums() {
        val entity = TransactionEntity(
            id = "tx_123",
            amountPaise = 250050L, // ₹2,500.50
            currencyCode = "INR",
            direction = "CREDIT",
            merchant = "Acme Corp",
            note = "Consulting invoice #42",
            categoryId = "INCOME",
            occurredAtEpochMillis = 1700000000000L,
            sourceType = "MANUAL",
            sourcePackage = null,
            confidence = 1.0f,
            reviewStatus = "CONFIRMED",
            dedupeKey = "dedupe_123",
            createdAtEpochMillis = 1700000000000L,
            updatedAtEpochMillis = 1700000000000L
        )

        val domain = entity.toDomain()
        assertEquals("tx_123", domain.id)
        assertEquals(250050L, domain.amountPaise)
        assertEquals("INR", domain.currencyCode)
        assertEquals(TransactionDirection.CREDIT, domain.direction)
        assertEquals("Acme Corp", domain.merchant)
        assertEquals("Consulting invoice #42", domain.note)
        assertEquals(TransactionCategory.INCOME, domain.category)
        assertEquals(1700000000000L, domain.occurredAtEpochMillis)
        assertEquals(SourceType.MANUAL, domain.sourceType)
        assertEquals(ReviewStatus.CONFIRMED, domain.reviewStatus)
        assertEquals("dedupe_123", domain.dedupeKey)

        // And convert back to Entity
        val remapped = domain.toEntity()
        assertEquals(entity, remapped)
    }

    @Test
    fun csvExporter_formatsHeaderAndRowsCorrectly() {
        val txList = listOf(
            Transaction(
                id = "tx_1",
                amountPaise = 35000L, // 350.00
                direction = TransactionDirection.DEBIT,
                merchant = "Coffee Shop, Roasters", // contains comma
                note = "Double espresso",
                category = TransactionCategory.FOOD,
                occurredAtEpochMillis = 1700000000000L, // 2023-11-14
                sourceType = SourceType.NOTIFICATION
            ),
            Transaction(
                id = "tx_2",
                amountPaise = 2500000L, // 25000.00
                direction = TransactionDirection.CREDIT,
                merchant = "Salary",
                note = null,
                category = TransactionCategory.INCOME,
                occurredAtEpochMillis = 1700000000000L,
                sourceType = SourceType.MANUAL
            )
        )

        val csv = CsvExporter.generateCsv(txList)
        val lines = csv.trim().split("\n")

        // Check header
        assertEquals("date,direction,amount_inr,merchant,category,note,source", lines[0])

        // Check row 1 with comma escaping
        assertTrue(lines[1].contains("DEBIT,350.00,\"Coffee Shop, Roasters\",FOOD,Double espresso,NOTIFICATION"))

        // Check row 2
        assertTrue(lines[2].contains("CREDIT,25000.00,Salary,INCOME,,MANUAL"))

        // Check write to stream
        val out = ByteArrayOutputStream()
        CsvExporter.writeCsvToStream(txList, out)
        val streamResult = out.toString("UTF-8")
        assertEquals(csv, streamResult)

        // Verify sensitive fields are not exported
        assertFalse(csv.contains("tx_1"))
        assertFalse(csv.contains("tx_2"))
        assertFalse(csv.contains("dedupeKey"))
    }
}
