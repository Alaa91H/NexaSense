package com.nexasense.presentation.theme

import androidx.compose.animation.core.CubicBezierEasing

/**
 * Material 3 motion tokens used by every animation in the app.
 *
 * material3 1.4.0 ships `androidx.compose.material3.tokens.MotionTokens`, but
 * it is `internal` to the artifact, so its values are mirrored here: the
 * durations come from the published M3 motion spec, and the easing curves
 * were verified directly against the 1.4.0 artifact's bytecode. Keeping one
 * local source means every transition (accordions, dialogs, indicators)
 * animates on the same official M3 2026 curves and durations instead of
 * ad-hoc tweens.
 */
object Motion {
    /** M3 duration tokens (ms), from the motion spec. */
    const val DurationShort4 = 200
    const val DurationMedium2 = 300
    const val DurationMedium4 = 400

    /** M3 easing curves (verified against material3 1.4.0 bytecode). */
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val StandardAccelerate = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    val StandardDecelerate = CubicBezierEasing(0f, 0f, 0f, 1f)
}
