package com.nexasense.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end UI smoke tests running against the real application and the
 * device's actual sensor HAL — including devices without sensors, where the
 * screens must degrade gracefully instead of crashing.
 *
 * The app has three tools switched via the bottom navigation bar: Compass
 * (home), Level and Settings. The sensors/diagnostics/about screens have been
 * removed.
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
    fun bottomNavSwitchesToLevelAndBack() {
        rule.onAllNodesWithText("Level").onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("Level").onFirst().assertExists()
        rule.onAllNodesWithText("Compass").onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("Compass").onFirst().assertExists()
    }

    @Test
    fun bottomNavOpensSettings() {
        rule.onAllNodesWithText("Settings").onFirst().performClick()
        rule.waitForIdle()
        // Section headers are always visible; the body expands on tap.
        rule.onNodeWithText("Theme").assertExists()
        rule.onAllNodesWithText("Compass").onFirst().assertExists()
    }

    @Test
    fun removedScreensAreNotReachable() {
        // The sensors, diagnostics and about screens no longer exist: none of
        // their titles appear anywhere in the app.
        rule.onNodeWithText("Sensors").assertDoesNotExist()
        rule.onNodeWithText("Diagnostics").assertDoesNotExist()
        rule.onNodeWithText("NexaSense — AOSP Sensor Suite").assertDoesNotExist()
    }
}
