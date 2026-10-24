package com.spendwise.android

import com.spendwise.android.ui.demo.DemoData
import com.spendwise.android.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Phase 1 Navigation routes and Demo data invariants.
 */
class Phase1NavigationAndDemoUnitTest {

    @Test
    fun screenRoutes_areProperlyDefined() {
        assertEquals("onboarding", Screen.Onboarding.route)
        assertEquals("dashboard", Screen.Dashboard.route)
        assertEquals("transactions", Screen.Transactions.route)
        assertEquals("review", Screen.Review.route)
        assertEquals("settings", Screen.Settings.route)
        assertEquals("budget", Screen.Budget.route)
        assertEquals("notification_setup", Screen.NotificationSetup.route)
        assertEquals("monitored_apps", Screen.MonitoredApps.route)

        // Route creation helper
        assertEquals("add_edit_transaction", Screen.AddEditTransaction.createRoute())
        assertEquals("add_edit_transaction?transactionId=tx_123", Screen.AddEditTransaction.createRoute("tx_123"))

        // Bottom nav items
        assertEquals(4, Screen.bottomNavItems.size)
        assertTrue(Screen.bottomNavItems.contains(Screen.Dashboard))
        assertTrue(Screen.bottomNavItems.contains(Screen.Transactions))
        assertTrue(Screen.bottomNavItems.contains(Screen.Review))
        assertTrue(Screen.bottomNavItems.contains(Screen.Settings))

        // Ensure no item is null and routes are valid (regression guard against circular class initialization)
        for (item in Screen.bottomNavItems) {
            assertNotNull("Bottom nav item must never be null", item)
            assertTrue("Bottom nav item route must not be blank", item.route.isNotBlank())
            assertTrue("Bottom nav item title must not be blank", item.title.isNotBlank())
        }
    }

    @Test
    fun demoTransactions_haveValidMonetaryAmountsAndFields() {
        assertTrue(DemoData.sampleTransactions.isNotEmpty())
        for (tx in DemoData.sampleTransactions) {
            assertTrue("Transaction amount must be strictly positive", tx.amountPaise > 0L)
            assertTrue("Merchant must not be blank", tx.merchant.isNotBlank())
            assertNotNull(tx.direction)
            assertNotNull(tx.category)
            assertNotNull(tx.sourceType)
        }
    }

    @Test
    fun demoReviewCandidates_haveValidScoresAndUncertaintyReasons() {
        assertTrue(DemoData.sampleReviewCandidates.isNotEmpty())
        for (cand in DemoData.sampleReviewCandidates) {
            assertTrue("Confidence score must be between 0.0 and 1.0", cand.confidenceScore in 0.0f..1.0f)
            assertTrue("Uncertainty reason must not be blank", cand.uncertaintyReason.isNotBlank())
            assertTrue("Source app must not be blank", cand.sourceAppName.isNotBlank())
        }
    }

    @Test
    fun demoMonitoredApps_haveValidPackages() {
        assertTrue(DemoData.sampleMonitoredApps.isNotEmpty())
        for (app in DemoData.sampleMonitoredApps) {
            assertTrue("Package name must contain dots", app.packageName.contains("."))
            assertTrue("App name must not be blank", app.appName.isNotBlank())
        }
    }
}
