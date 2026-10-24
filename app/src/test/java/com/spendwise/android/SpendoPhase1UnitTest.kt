package com.spendwise.android

import com.spendwise.android.ui.spendo.components.SpendoBottomNavItem
import com.spendwise.android.ui.spendo.model.AiProvider
import com.spendwise.android.ui.spendo.model.SpendoMockRepository
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.navigation.SpendoScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Phase 1 requirements for Spendo — Your AI Money Agent.
 */
class SpendoPhase1UnitTest {

    @Test
    fun spendoScreens_all17RoutesAreProperlyConfigured() {
        // 4 Bottom Navigation destinations
        assertEquals("spendo_home", SpendoScreen.Home.route)
        assertEquals("spendo_activity", SpendoScreen.Activity.route)
        assertEquals("spendo_calendar", SpendoScreen.Calendar.route)
        assertEquals("spendo_ai_chat", SpendoScreen.SpendoAi.route)

        // Secondary feature screens
        assertEquals("spendo_notifications", SpendoScreen.Notifications.route)
        assertEquals("spendo_add_transaction", SpendoScreen.AddTransaction.route)
        assertEquals("spendo_transaction_detail/{transactionId}", SpendoScreen.TransactionDetail.route)
        assertEquals("spendo_transaction_detail/tx_99", SpendoScreen.TransactionDetail.createRoute("tx_99"))
        assertEquals("spendo_budget", SpendoScreen.Budget.route)
        assertEquals("spendo_analytics", SpendoScreen.Analytics.route)
        assertEquals("spendo_banks", SpendoScreen.Banks.route)
        assertEquals("spendo_income", SpendoScreen.Income.route)
        assertEquals("spendo_streaks", SpendoScreen.Streaks.route)
        assertEquals("spendo_ai_config", SpendoScreen.AiConfig.route)
        assertEquals("spendo_notification_settings", SpendoScreen.NotificationSettings.route)
        assertEquals("spendo_import_export", SpendoScreen.ImportExport.route)
        assertEquals("spendo_settings", SpendoScreen.Settings.route)
        assertEquals("spendo_security", SpendoScreen.Security.route)
    }

    @Test
    fun spendoBottomNav_containsExactlyFourDestinations() {
        val items = SpendoBottomNavItem.values()
        assertEquals(4, items.size)
        assertEquals(SpendoBottomNavItem.HOME, items[0])
        assertEquals(SpendoBottomNavItem.ACTIVITY, items[1])
        assertEquals(SpendoBottomNavItem.CALENDAR, items[2])
        assertEquals(SpendoBottomNavItem.SPENDO_AI, items[3])

        items.forEach { item ->
            assertTrue("Nav item title must not be blank", item.title.isNotBlank())
            assertTrue("Nav item route must not be blank", item.route.isNotBlank())
            assertNotNull("Nav item icon must not be null", item.icon)
        }
    }

    @Test
    fun spendoAi_defaultStateIsUnconfiguredAsRequired() {
        val defaultState = SpendoUiState()
        assertFalse(
            "Spendo AI must appear as unconfigured by default until user sets API key",
            defaultState.aiConfig.isConfigured
        )
        assertTrue("Default API key should be blank", defaultState.aiConfig.apiKey.isBlank())
        assertNotNull("Default provider must be set", defaultState.aiConfig.provider)
    }

    @Test
    fun spendoAiProviders_containsAllRequiredOptions() {
        val providers = AiProvider.values().map { it.displayName }
        assertTrue(providers.contains("Groq"))
        assertTrue(providers.contains("Cerebras"))
        assertTrue(providers.contains("Google Gemini"))
        assertTrue(providers.contains("Sarvam AI"))
        assertTrue(providers.contains("OpenAI"))
        assertTrue(providers.contains("Meta AI (Hosted)"))
    }

    @Test
    fun spendoMockData_containsValidBanksAndBalances() {
        assertTrue("Sample banks must not be empty", SpendoMockRepository.sampleBanks.isNotEmpty())
        for (bank in SpendoMockRepository.sampleBanks) {
            assertTrue("Bank balance must be positive", bank.balancePaise > 0L)
            assertTrue("Masked account must contain dots", bank.maskedAccountNumber.contains("••••"))
            assertTrue("Bank name must not be blank", bank.bankName.isNotBlank())
        }
    }

    @Test
    fun spendoMockData_containsRealisticTransactions() {
        assertTrue(SpendoMockRepository.sampleTransactions.isNotEmpty())
        for (tx in SpendoMockRepository.sampleTransactions) {
            assertTrue("Amount must be positive", tx.amountPaise > 0L)
            assertTrue("Merchant must not be blank", tx.merchant.isNotBlank())
            assertNotNull(tx.type)
            assertNotNull(tx.source)
        }
    }

    @Test
    fun spendoMockData_containsCategorizedNotifications() {
        assertTrue(SpendoMockRepository.sampleNotifications.isNotEmpty())
        val categories = SpendoMockRepository.sampleNotifications.map { it.category }.toSet()
        assertTrue(categories.contains("Transactions"))
        assertTrue(categories.contains("Budgets"))
        assertTrue(categories.contains("Motivation"))
    }
}
