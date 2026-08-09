package com.nexasense.domain.engine

/**
 * Caches geomagnetic declination so the (comparatively expensive)
 * `GeomagneticField` model is evaluated only when the location or the time
 * changes significantly — never per sensor event.
 *
 * Pure logic: the actual model call is injected via [compute].
 */
class DeclinationCache(
    private val maxAgeMillis: Long = 10 * 60_000L,
    private val minMoveMeters: Double = 1_000.0,
) {

    private var cachedLatitude = Double.NaN
    private var cachedLongitude = Double.NaN
    private var cachedAtMillis = 0L
    private var cachedValue: Float? = null

    /**
     * Returns the cached declination when it is still fresh and the location
     * has not moved materially; otherwise computes a new one and caches it.
     *
     * @param nowMillis wall-clock time used for the age check.
     */
    fun declination(
        latitudeDegrees: Double,
        longitudeDegrees: Double,
        altitudeMeters: Double,
        timeMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        compute: (Double, Double, Double, Long) -> Float?,
    ): Float? {
        val moved = if (cachedLatitude.isNaN()) {
            true
        } else {
            QiblaCalculator.greatCircleDistance(
                cachedLatitude, cachedLongitude, latitudeDegrees, longitudeDegrees,
            ) > minMoveMeters
        }
        val fresh = cachedValue != null && (nowMillis - cachedAtMillis) < maxAgeMillis && !moved
        if (fresh) return cachedValue

        val value = compute(latitudeDegrees, longitudeDegrees, altitudeMeters, timeMillis)
        cachedValue = value
        cachedLatitude = latitudeDegrees
        cachedLongitude = longitudeDegrees
        cachedAtMillis = nowMillis
        return value
    }

    fun clear() {
        cachedLatitude = Double.NaN
        cachedLongitude = Double.NaN
        cachedAtMillis = 0L
        cachedValue = null
    }
}
