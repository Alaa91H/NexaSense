package com.nexasense.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
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
 *
 * The compass is the app's home screen; the dashboard (with Level, Sensors,
 * Diagnostics, Settings, About) is reached through the menu button.
 */
@RunWith(AndroidJUnit4::class)
class NexaSenseUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun compassIsTheHomeScreen() {
        // The app opens directly on the compass, which acts as the home page.
        rule.onAllNodesWithText("Compass").onFirst().assertExists()
    }

    @Test
    fun menuOpensDashboard() {
        rule.onNodeWithContentDescription("Home").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("NexaSense", substring = true).assertIsDisplayed()
        rule.onAllNodesWithText("Level").onFirst().assertExists()
        rule.onAllNodesWithText("Sensors").onFirst().assertExists()
    }

    @Test
    fun navigateFromMenuToLevelAndBack() {
        rule.onNodeWithContentDescription("Home").performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("Level").onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("Level").onFirst().assertExists()
        Espresso.pressBack()
        rule.waitForIdle()
        rule.onNodeWithText("NexaSense", substring = true).assertIsDisplayed()
    }

    @Test
    fun navigateToSensorsList() {
        rule.onNodeWithContentDescription("Home").performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("Sensors").onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("Sensors").onFirst().assertExists()
    }

    @Test
    fun navigateToSettingsAndAbout() {
        rule.onNodeWithContentDescription("Home").performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("Settings").onFirst().performClick()
        rule.waitForIdle()
        // Section headers are always visible; the body expands on tap.
        rule.onNodeWithText("Theme").assertExists()
        rule.onAllNodesWithText("About").onFirst().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("NexaSense — AOSP Sensor Suite").assertIsDisplayed()
    }
}
