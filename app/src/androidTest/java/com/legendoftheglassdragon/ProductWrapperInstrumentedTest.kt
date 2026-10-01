package com.legendoftheglassdragon

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductWrapperInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchPresentsProductionMainMenuWithoutLegacyConnectionGate() {
        composeRule.onNodeWithContentDescription(
            "Pixel ANSI dragon, crystal, and sword title ornament",
        ).assertExists()
        composeRule.onNodeWithText("New Game", substring = true).assertExists()
        composeRule.onNodeWithText("Settings", substring = true).assertExists()
        composeRule.onNodeWithText("Credits", substring = true).assertExists()
        composeRule.onNodeWithText("CONNECTING...", substring = true).assertDoesNotExist()
    }
}
