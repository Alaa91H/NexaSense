package com.nexasense.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Centralized Material 3 Expressive shapes. Consistent corner radii across
 * cards, dialogs, banners and the compass/level surfaces, following the
 * rounder, softer shape scale Google introduced with Material 3 Expressive
 * (2025–2026): containers are noticeably more rounded than the classic M3
 * scale, and large surfaces (dialogs, cards) use near-pill corners.
 */
val NexaSenseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
