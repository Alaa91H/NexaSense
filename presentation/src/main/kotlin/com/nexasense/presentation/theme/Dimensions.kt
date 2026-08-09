package com.nexasense.presentation.theme

import androidx.compose.ui.unit.dp

/**
 * Centralized spacing/sizing scale. All screens use these instead of inline
 * magic numbers, keeping phones, tablets and foldables consistent.
 */
object NexaSenseDimensions {

    /** Minimum touch target per Material guidelines. */
    val TouchTargetMin = 48.dp

    val SpacingXs = 4.dp
    val SpacingSm = 8.dp
    val SpacingMd = 12.dp
    val SpacingLg = 16.dp
    val SpacingXl = 24.dp

    val ScreenPadding = SpacingXl
    val CardContentPadding = SpacingLg

    val DialMaxSize = 360.dp
}
