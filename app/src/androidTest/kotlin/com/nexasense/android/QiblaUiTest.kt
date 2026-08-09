package com.nexasense.android

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the North Reference and Qibla settings. The location
 * permission is granted for these tests so the Qibla flow can be exercised
 * end-to-end; every test starts from a reset settings state.
 */
@RunWith(AndroidJUnit4::class)
class QiblaUiTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.ACCESS_COARSE_LOCATION)

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetSettings() {
        rule.onAllNodesWithText("Settings").onFirst().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Reset settings").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("OK").performClick()
        rule.waitForIdle()
        Espresso.pressBack()
        rule.waitForIdle()
    }

    @Test
    fun settingsShowsNorthReferenceOptions() {
        rule.onAllNodesWithText("Settings").onFirst().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("North Reference").assertIsDisplayed()
        rule.onNodeWithText("Automatic").assertIsDisplayed()
        rule.onNodeWithText("True North").assertIsDisplayed()
        rule.onNodeWithText("Magnetic North").assertIsDisplayed()
    }

    @Test
    fun qiblaIsDisabledByDefault() {
        rule.onAllNodesWithText("Settings").onFirst().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Qibla Direction").assertIsDisplayed()
        rule.onNodeWithText("Enable Qibla").assertIsOff()
        // Sub-options are hidden until Qibla is enabled.
        rule.onNodeWithText("Show on Compass").assertDoesNotExist()
        rule.onNodeWithText("Show Qibla card").assertDoesNotExist()
    }

    @Test
    fun enablingQiblaRevealsSubOptions() {
        rule.onAllNodesWithText("Settings").onFirst().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Enable Qibla").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Show on Compass").assertIsDisplayed()
        rule.onNodeWithText("Show Qibla card").assertIsDisplayed()
        rule.onNodeWithText("Show distance to Kaaba").assertIsDisplayed()
        rule.onNodeWithText("Haptic feedback on alignment").assertIsDisplayed()
    }

    @Test
    fun disablingQiblaHidesSubOptions() {
        rule.onAllNodesWithText("Settings").onFirst().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Enable Qibla").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Show on Compass").assertIsDisplayed()
        rule.onNodeWithText("Enable Qibla").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Show on Compass").assertDoesNotExist()
    }
}
