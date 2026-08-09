package com.nexasense.domain.model

/** Current magnetic field state exposed to the compass UI. */
data class MagneticFieldState(
    val x: Float,
    val y: Float,
    val z: Float,
    val magnitudeMicroTesla: Float,
    val accuracy: AccuracyLevel,
    val interference: Boolean,
    val bandViolation: Boolean,
    val baselineMicroTesla: Float,
) {
    val isCalibratedEnough: Boolean get() = accuracy != AccuracyLevel.UNRELIABLE

    companion object {
        val NONE: MagneticFieldState = MagneticFieldState(
            x = 0f,
            y = 0f,
            z = 0f,
            magnitudeMicroTesla = 0f,
            accuracy = AccuracyLevel.UNRELIABLE,
            interference = false,
            bandViolation = false,
            baselineMicroTesla = 0f,
        )
    }
}
