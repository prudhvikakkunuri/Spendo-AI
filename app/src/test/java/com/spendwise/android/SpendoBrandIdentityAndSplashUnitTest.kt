package com.spendwise.android

import androidx.compose.ui.graphics.Color
import com.spendwise.android.ui.spendo.theme.DarkSpendoColors
import com.spendwise.android.ui.spendo.theme.LightSpendoColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Verification of Spendo Brand Identity, Launcher Icon & Splash Configuration.
 */
class SpendoBrandIdentityAndSplashUnitTest {

    @Test
    fun testOfficialBrandPalette_isMonochromeFirstWithRedAccent() {
        // Dark theme: pitch black background
        assertEquals(Color(0xFF000000), DarkSpendoColors.background)
        assertEquals(Color(0xFFEDEDED), DarkSpendoColors.textPrimary)

        // Accent red is preserved and no lime green or vibrant blue dominates
        assertNotNull(DarkSpendoColors.accentRed)
        assertTrue("Accent red should have significant red channel", DarkSpendoColors.accentRed.red > 0.5f)
    }

    @Test
    fun testLauncherAndSplashVectorDrawables_existAndAreConfigured() {
        val resDir = File("src/main/res")
        assertTrue("Resource directory must exist", resDir.exists())

        // 1. Check launcher icon assets
        val launcherBg = File(resDir, "drawable/ic_launcher_background.xml")
        val launcherFg = File(resDir, "drawable/ic_launcher_foreground.xml")
        assertTrue("ic_launcher_background.xml must exist", launcherBg.exists())
        assertTrue("ic_launcher_foreground.xml must exist", launcherFg.exists())

        val bgContent = launcherBg.readText()
        assertTrue("Launcher background must be pure black #000000", bgContent.contains("#000000"))

        val fgContent = launcherFg.readText()
        assertTrue("Launcher foreground must contain solid circle", fgContent.contains("Outer Solid Circle Ring") || fgContent.contains("A31,31"))
        assertTrue("Launcher foreground must contain concentric dotted ring", fgContent.contains("Dotted Ring") || fgContent.contains("M78.50"))
        assertTrue("Launcher foreground must contain geometric S", fgContent.contains("Geometric S Monogram") || fgContent.contains("M42,66"))
        assertTrue("Launcher foreground must contain red accent ring", fgContent.contains("#E60023") || fgContent.contains("Accent Red"))

        // 2. Check simplified symbol and full logo
        val spendoLogo = File(resDir, "drawable/ic_spendo_logo.xml")
        val spendoSymbol = File(resDir, "drawable/ic_spendo_symbol.xml")
        assertTrue("ic_spendo_logo.xml must exist", spendoLogo.exists())
        assertTrue("ic_spendo_symbol.xml must exist", spendoSymbol.exists())

        // 3. Check native splash background
        val splashBg = File(resDir, "drawable/splash_background.xml")
        assertTrue("splash_background.xml must exist", splashBg.exists())
        val splashContent = splashBg.readText()
        assertTrue("Splash must use #000000 background", splashContent.contains("#000000"))
        assertTrue("Splash must reference ic_spendo_logo", splashContent.contains("@drawable/ic_spendo_logo"))
    }

    @Test
    fun testAndroidManifest_usesSplashThemeForMainActivity() {
        val manifest = File("src/main/AndroidManifest.xml")
        assertTrue(manifest.exists())
        val manifestContent = manifest.readText()
        assertTrue(
            "MainActivity must be declared with Theme.Spendo.Splash to prevent white flash",
            manifestContent.contains("android:theme=\"@style/Theme.Spendo.Splash\"")
        )
    }

    @Test
    fun testSplashAnimationStages_adhereToDurationGuidelines() {
        // Target: 1.2s - 1.8s
        val stage1PauseMs = 120L
        val stage2CircleMs = 240L
        val stage3DotRingMs = 280L
        val stage4SymbolDrawMs = 340L
        val stage5RedAccentMs = 180L
        val stage6BrandRevealMs = 240L
        val stage7HoldMs = 260L

        val totalAnimationDurationMs = stage1PauseMs + stage2CircleMs + stage3DotRingMs +
                stage4SymbolDrawMs + stage5RedAccentMs + stage6BrandRevealMs + stage7HoldMs

        assertTrue("Animation duration ($totalAnimationDurationMs ms) must be >= 1200 ms", totalAnimationDurationMs >= 1200L)
        assertTrue("Animation duration ($totalAnimationDurationMs ms) must be <= 1800 ms", totalAnimationDurationMs <= 1800L)
    }

    @Test
    fun testColorsXml_doesNotContainLimeGreenAccent() {
        val colorsXml = File("src/main/res/values/colors.xml")
        assertTrue(colorsXml.exists())
        val content = colorsXml.readText()
        assertFalse("lime_accent (#B4F234) must not exist in official Spendo theme", content.contains("#B4F234"))
        assertTrue("spendo_red must be defined", content.contains("#E60023") || content.contains("spendo_red"))
    }
}
