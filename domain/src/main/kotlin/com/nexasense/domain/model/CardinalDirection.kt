package com.nexasense.domain.model

/** The eight wind compass directions. */
enum class CardinalDirection(val degrees: Int) {
    N(0),
    NE(45),
    E(90),
    SE(135),
    S(180),
    SW(225),
    W(270),
    NW(315);

    companion object {
        /**
         * Returns the cardinal direction for a heading in degrees [0, 360).
         * Boundaries fall exactly on the cardinal angles.
         */
        fun fromDegrees(degrees: Float): CardinalDirection {
            val normalized = ((degrees % 360f) + 360f) % 360f
            val index = ((normalized + 22.5f) / 45f).toInt() % 8
            return entries[index.coerceIn(0, 7)]
        }
    }
}
