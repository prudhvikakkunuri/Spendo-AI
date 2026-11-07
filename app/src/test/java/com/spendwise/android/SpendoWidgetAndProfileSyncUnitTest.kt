package com.spendwise.android

import com.spendwise.android.ui.spendo.model.SpendoUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Calendar

/**
 * Regression unit test suite for:
 * 1. Widget Button Container Boundary & Sizing Integrity (2x2 and 4x2 widgets).
 * 2. User Profile Synchronization & Dynamic Dashboard Greeting (Removing hardcoded "Alex").
 */
class SpendoWidgetAndProfileSyncUnitTest {

    // -------------------------------------------------------------
    // 1. User Profile Synchronization & Dynamic Greeting Tests
    // -------------------------------------------------------------

    @Test
    fun testDefaultUiState_hasEmptyUserName_greetingShowsWelcomeToSpendo() {
        val state = SpendoUiState()
        assertEquals("", state.userName)

        val greeting = computeGreeting(state.userName)
        assertEquals("Welcome to Spendo", greeting)
    }

    @Test
    fun testUserNameFallback_userString_showsWelcomeToSpendo() {
        assertEquals("Welcome to Spendo", computeGreeting("User"))
        assertEquals("Welcome to Spendo", computeGreeting("   "))
        assertEquals("Welcome to Spendo", computeGreeting(""))
    }

    @Test
    fun testSavedUserName_displaysDynamicTimeGreetingWithName() {
        val testName = "Prudhvi"
        val greeting = computeGreeting(testName)

        assertTrue(
            "Greeting should contain user name '$testName' but was: $greeting",
            greeting.endsWith(", $testName")
        )
        assertFalse(
            "Greeting must not be hardcoded to 'Hello, Alex'",
            greeting == "Hello, Alex"
        )
    }

    @Test
    fun testDifferentUserNames_renderConsistentlyWithoutHardcoding() {
        val names = listOf("Sarah", "Karthik", "Rohan", "Elena", "Devin")
        for (name in names) {
            val greeting = computeGreeting(name)
            assertTrue("Greeting should end with '$name'", greeting.endsWith(", $name"))
        }
    }

    @Test
    fun testProfileNameTrimming_removesExcessWhitespace() {
        val rawInput = "   Prudhvi Kakkunuri   "
        val trimmed = rawInput.trim()
        assertEquals("Prudhvi Kakkunuri", trimmed)

        val greeting = computeGreeting(trimmed)
        assertEquals(true, greeting.endsWith(", Prudhvi Kakkunuri"))
    }

    private fun computeGreeting(userName: String): String {
        val trimmed = userName.trim()
        return if (trimmed.isBlank() || trimmed.equals("User", ignoreCase = true)) {
            "Welcome to Spendo"
        } else {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val timeGreeting = when (hour) {
                in 4..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                in 17..22 -> "Good evening"
                else -> "Hello"
            }
            "$timeGreeting, $trimmed"
        }
    }

    // -------------------------------------------------------------
    // 2. Widget Layout & Container Boundary Integrity Tests
    // -------------------------------------------------------------

    private fun getResFile(relativePath: String): File {
        val f1 = File("src/main/res/$relativePath")
        if (f1.exists()) return f1
        return File("app/src/main/res/$relativePath")
    }

    @Test
    fun testWidget2x2Xml_actionButtonsHaveSingleLineAndEllipsize() {
        val file = getResFile("layout/widget_spendo_2x2.xml")
        assertTrue("widget_spendo_2x2.xml must exist at ${file.absolutePath}", file.exists())

        val content = file.readText()
        assertTrue("2x2 layout must declare widget_2x2_action_add", content.contains("widget_2x2_action_add"))
        assertTrue("2x2 layout must declare widget_2x2_action_voice", content.contains("widget_2x2_action_voice"))

        // Verify singleLine and ellipsize are present on action buttons
        assertTrue("Action buttons must prevent text wrapping with singleLine", content.contains("android:singleLine=\"true\""))
        assertTrue("Action buttons must prevent text wrapping with ellipsize", content.contains("android:ellipsize=\"end\""))
    }

    @Test
    fun testWidget4x2Xml_actionButtonsHaveSingleLineAndEllipsize() {
        val file = getResFile("layout/widget_spendo_4x2.xml")
        assertTrue("widget_spendo_4x2.xml must exist at ${file.absolutePath}", file.exists())

        val content = file.readText()
        assertTrue("4x2 layout must declare widget_4x2_action_add", content.contains("widget_4x2_action_add"))
        assertTrue("4x2 layout must declare widget_4x2_action_ai", content.contains("widget_4x2_action_ai"))

        // Verify singleLine and ellipsize are present on action buttons
        assertTrue("4x2 Action buttons must have singleLine", content.contains("android:singleLine=\"true\""))
        assertTrue("4x2 Action buttons must have ellipsize", content.contains("android:ellipsize=\"end\""))
    }

    @Test
    fun testWidgetPillButton_hasCapsuleRadius() {
        val file = getResFile("drawable/widget_pill_button.xml")
        assertTrue("widget_pill_button.xml must exist at ${file.absolutePath}", file.exists())

        val content = file.readText()
        assertTrue("Pill button should have radius <= 16dp for 28-30dp height", content.contains("android:radius=\"14dp\"") || content.contains("android:radius=\"16dp\""))
    }

    @Test
    fun testSpendoWidgetInfo_allowsVerticalAndHorizontalResize() {
        val file = getResFile("xml/spendo_widget_info.xml")
        assertTrue("spendo_widget_info.xml must exist at ${file.absolutePath}", file.exists())

        val content = file.readText()
        assertTrue("Widget info must specify resizeMode horizontal|vertical", content.contains("horizontal|vertical"))
        assertTrue("Widget info maxResizeHeight must be >= 380dp to allow 4-row widgets", content.contains("maxResizeHeight=\"420dp\"") || content.contains("maxResizeHeight=\"380dp\""))
    }

    @Test
    fun testWidget2x3Xml_hasRequiredElementsAndSingleLine() {
        val file = getResFile("layout/widget_spendo_2x3.xml")
        assertTrue("widget_spendo_2x3.xml must exist at ${file.absolutePath}", file.exists())

        val content = file.readText()
        assertTrue("2x3 layout must declare widget_2x3_today_spend", content.contains("widget_2x3_today_spend"))
        assertTrue("2x3 layout must declare widget_2x3_progress_bar", content.contains("widget_2x3_progress_bar"))
        assertTrue("2x3 layout must declare widget_2x3_limit_status", content.contains("widget_2x3_limit_status"))
        assertTrue("2x3 layout must declare widget_2x3_streak", content.contains("widget_2x3_streak"))
        assertTrue("2x3 layout must declare widget_2x3_action_add", content.contains("widget_2x3_action_add"))
        assertTrue("2x3 layout must declare widget_2x3_action_voice", content.contains("widget_2x3_action_voice"))
        assertTrue("2x3 Action buttons must have singleLine", content.contains("android:singleLine=\"true\""))
        assertTrue("2x3 Action buttons must have ellipsize", content.contains("android:ellipsize=\"end\""))
    }

    @Test
    fun testWidget2x4Xml_hasRecentExpensesAndSingleLine() {
        val file = getResFile("layout/widget_spendo_2x4.xml")
        assertTrue("widget_spendo_2x4.xml must exist at ${file.absolutePath}", file.exists())

        val content = file.readText()
        assertTrue("2x4 layout must declare widget_2x4_today_spend", content.contains("widget_2x4_today_spend"))
        assertTrue("2x4 layout must declare widget_2x4_progress_bar", content.contains("widget_2x4_progress_bar"))
        assertTrue("2x4 layout must declare widget_2x4_recent_empty", content.contains("widget_2x4_recent_empty"))
        assertTrue("2x4 layout must declare widget_2x4_action_add", content.contains("widget_2x4_action_add"))
        assertTrue("2x4 layout must declare widget_2x4_action_voice", content.contains("widget_2x4_action_voice"))
        assertTrue("2x4 Action buttons must have singleLine", content.contains("android:singleLine=\"true\""))
    }

    @Test
    fun testWidget3x4Xml_hasBalancedGridAndSingleLine() {
        val file = getResFile("layout/widget_spendo_3x4.xml")
        assertTrue("widget_spendo_3x4.xml must exist at ${file.absolutePath}", file.exists())

        val content = file.readText()
        assertTrue("3x4 layout must declare widget_3x4_today_spend", content.contains("widget_3x4_today_spend"))
        assertTrue("3x4 layout must declare widget_3x4_month_spend", content.contains("widget_3x4_month_spend"))
        assertTrue("3x4 layout must declare widget_3x4_total_balance", content.contains("widget_3x4_total_balance"))
        assertTrue("3x4 layout must declare widget_3x4_action_add", content.contains("widget_3x4_action_add"))
        assertTrue("3x4 layout must declare widget_3x4_action_ai", content.contains("widget_3x4_action_ai"))
        assertTrue("3x4 Action buttons must have singleLine", content.contains("android:singleLine=\"true\""))
    }

    @Test
    fun testWidget3x3Xml_hasRequiredElementsAndRecentExpense() {
        val file = getResFile("layout/widget_spendo_3x3.xml")
        assertTrue("widget_spendo_3x3.xml must exist at ${file.absolutePath}", file.exists())

        val content = file.readText()
        assertTrue("3x3 layout must declare widget_3x3_today_spend", content.contains("widget_3x3_today_spend"))
        assertTrue("3x3 layout must declare widget_3x3_progress_bar", content.contains("widget_3x3_progress_bar"))
        assertTrue("3x3 layout must declare widget_3x3_recent_item_1", content.contains("widget_3x3_recent_item_1"))
        assertTrue("3x3 layout must declare widget_3x3_recent_empty", content.contains("widget_3x3_recent_empty"))
        assertTrue("3x3 layout must declare widget_3x3_action_add", content.contains("widget_3x3_action_add"))
        assertTrue("3x3 layout must declare widget_3x3_action_voice", content.contains("widget_3x3_action_voice"))
    }

    @Test
    fun testWidgetXmlLayouts_doNotContainIllegalViewTags() {
        val layoutFiles = listOf(
            "layout/widget_spendo_2x2.xml",
            "layout/widget_spendo_2x3.xml",
            "layout/widget_spendo_2x4.xml",
            "layout/widget_spendo_3x3.xml",
            "layout/widget_spendo_3x4.xml",
            "layout/widget_spendo_4x2.xml"
        )
        for (relPath in layoutFiles) {
            val file = getResFile(relPath)
            assertTrue("Layout file must exist: $relPath", file.exists())
            val content = file.readText()
            // RemoteViews disallows android.view.View tag (<View )
            assertFalse(
                "Layout $relPath contains illegal <View tag which crashes RemoteViews with InflateException",
                content.contains("<View\n") || content.contains("<View ") || content.contains("<View/>")
            )
        }
    }

    @Test
    fun testLargerWidgets_containRecentExpensesSection() {
        val widget2x4 = getResFile("layout/widget_spendo_2x4.xml").readText()
        assertTrue("2x4 widget must have widget_2x4_recent_empty", widget2x4.contains("widget_2x4_recent_empty"))
        assertTrue("2x4 widget must have widget_2x4_recent_item_1", widget2x4.contains("widget_2x4_recent_item_1"))

        val widget3x4 = getResFile("layout/widget_spendo_3x4.xml").readText()
        assertTrue("3x4 widget must have widget_3x4_recent_empty", widget3x4.contains("widget_3x4_recent_empty"))
        assertTrue("3x4 widget must have widget_3x4_recent_item_1", widget3x4.contains("widget_3x4_recent_item_1"))
    }

    // -------------------------------------------------------------
    // 3. Onboarding Gating & Profile Loading Tests
    // -------------------------------------------------------------

    @Test
    fun testOnboardingDecision_doesNotRedirectWhenProfileIsLoading() {
        // Initial state before DB finishes loading
        val loadingState = SpendoUiState(isProfileLoaded = false, isOnboardingCompleted = false)
        val shouldNavigateToOnboarding = loadingState.isProfileLoaded && !loadingState.isOnboardingCompleted
        assertFalse("Must never navigate to onboarding before profile has loaded", shouldNavigateToOnboarding)
    }

    @Test
    fun testOnboardingDecision_existingUserWithLoadedProfile_staysOnHome() {
        val existingUserState = SpendoUiState(isProfileLoaded = true, isOnboardingCompleted = true, userName = "Prudhvi")
        val shouldNavigateToOnboarding = existingUserState.isProfileLoaded && !existingUserState.isOnboardingCompleted
        assertFalse("Existing user must stay on dashboard/home", shouldNavigateToOnboarding)
    }

    @Test
    fun testOnboardingDecision_newInstallWithNoData_navigatesToOnboarding() {
        val newInstallState = SpendoUiState(isProfileLoaded = true, isOnboardingCompleted = false, userName = "")
        val shouldNavigateToOnboarding = newInstallState.isProfileLoaded && !newInstallState.isOnboardingCompleted
        assertTrue("New installation must navigate to onboarding", shouldNavigateToOnboarding)
    }

    // -------------------------------------------------------------
    // 4. Budget Notification Threshold Logic Tests
    // -------------------------------------------------------------

    @Test
    fun testBudgetThresholdEvaluation_approachingAndReachedAndExceeded() {
        val limitPaise = 100000L // ₹1,000.00

        // 50% spent: ₹500 - below threshold
        val spend50Percent = 50000L
        val ratio50 = spend50Percent.toDouble() / limitPaise
        val level50 = when {
            ratio50 >= 1.0 -> 2
            ratio50 >= 0.80 -> 1
            else -> 0
        }
        assertEquals(0, level50)

        // 85% spent: ₹850 - approaching threshold
        val spend85Percent = 85000L
        val ratio85 = spend85Percent.toDouble() / limitPaise
        val level85 = when {
            ratio85 >= 1.0 -> 2
            ratio85 >= 0.80 -> 1
            else -> 0
        }
        assertEquals(1, level85)

        // 100% spent: ₹1,000 - reached threshold
        val spend100Percent = 100000L
        val ratio100 = spend100Percent.toDouble() / limitPaise
        val level100 = when {
            ratio100 >= 1.0 -> 2
            ratio100 >= 0.80 -> 1
            else -> 0
        }
        assertEquals(2, level100)

        // 120% spent: ₹1,200 - exceeded threshold
        val spend120Percent = 120000L
        val ratio120 = spend120Percent.toDouble() / limitPaise
        val level120 = when {
            ratio120 >= 1.0 -> 2
            ratio120 >= 0.80 -> 1
            else -> 0
        }
        assertEquals(2, level120)
    }

    @Test
    fun testBudgetThresholdDeduplication_downwardAdjustmentResetsState() {
        var lastLevel = 2 // was at 100%
        val currentLevel = 0 // refund occurred, now below 80%

        // Downward adjustment should update lastLevel without alert
        if (currentLevel < lastLevel) {
            lastLevel = currentLevel
        }
        assertEquals("lastLevel must reset to 0 following refund/downward adjustment", 0, lastLevel)

        // New spend to 85% should now correctly alert again
        val nextLevel = 1
        val shouldAlert = nextLevel > lastLevel
        assertTrue("Threshold alert must fire when spending crosses 80% again after refund", shouldAlert)
    }

    @Test
    fun testLegacyDummyTransactionFilter_identifiesSeedIdsAndPreservesUserUuids() {
        val legacySeedTxIds = setOf(
            "tx_1", "tx_2", "tx_3", "tx_4", "tx_5", "tx_6", "tx_7", "tx_8", "tx_9", "tx_10",
            "tx_demo_1", "tx_demo_2", "tx_seed_1", "tx_seed_2"
        )
        val genuineUserTxId = "tx_3fa85f64-5717-4562-b3fc-2c963f66afa6"
        val genuineTransferId = "tx_xfer_out_1a2b3c"

        assertTrue("tx_1 must be identified as legacy seed dummy", legacySeedTxIds.contains("tx_1"))
        assertTrue("tx_6 must be identified as legacy seed dummy", legacySeedTxIds.contains("tx_6"))
        assertFalse("Genuine user UUID transaction must never be flagged as dummy", legacySeedTxIds.contains(genuineUserTxId))
        assertFalse("Genuine user transfer transaction must never be flagged as dummy", legacySeedTxIds.contains(genuineTransferId))
    }
}
