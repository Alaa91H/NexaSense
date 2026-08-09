package com.nexasense.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end UI smoke tests running against the real application and the
 * device's actual sensor HAL — including devices without sensors, where the
 * screens must degrade gracefully instead of crashing.
 */
@RunWith(AndroidJUnit4::class)
class NexaSenseUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenShowsTitleAndFeatureEntries() {
        rule.onNodeWithText("NexaSense", substring = true).assertIsDisplayed()
        rule.onAllNodesWithText("Compass").onFirst().assertExists()
        rule.onAllNodesWithText("Level").onFirst().assertExists()
        rule.onAllNodesWithText("Sensors").onFirst().assertExists()
    }

    @Test
    fun navigateToCompassAndBack() {
        rule.onAllNodesWithText("Compass").onFirst().performClick()
        rule.waitForIdle()
        // The compass screen is rendered; on hardware without the required
        // sensors it shows the unavailable panel instead of crashing.
        rule.onAllNodesWithText("Compass").onFirst().assertExists()
        Espresso.pressBack()
        rule.waitForIdle()
        rule.onNodeWithText("NexaSense", substring = true).assertIsDisplayed()
    }

    @Test
    fun navigateToSensorsList() {
        rule.onAllNodesWithText("Sensors").onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("Sensors").onFirst().assertExists()
    }

    @Test
    fun navigateToSettingsAndAbout() {
        rule.onAllNodesWithText("Settings").onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("Theme").onFirst().assertExists()
        rule.onAllNodesWithText("About").onFirst().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("NexaSense — AOSP Sensor Suite").assertIsDisplayed()
    }
}
