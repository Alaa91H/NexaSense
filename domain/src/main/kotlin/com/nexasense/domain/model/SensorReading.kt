package com.nexasense.domain.model

/**
 * One raw sensor event.
 *
 * @param sensorId framework handle of the sensor that produced the event.
 * @param kind resolved sensor kind.
 * @param values event values in the same order the framework reports them
 *   (x, y, z[, w] for vector sensors). The array is owned by this instance and
 *   is safe to keep.
 * @param accuracy accuracy of the reading, see [AccuracyLevel].
 * @param timestampNanos event time in the framework's nanosecond clock.
 */
data class SensorReading(
    val sensorId: Int,
    val kind: SensorKind,
    val values: FloatArray,
    val accuracy: AccuracyLevel,
    val timestampNanos: Long,
) {
    val x: Float get() = values.getOrElse(0) { 0f }
    val y: Float get() = values.getOrElse(1) { 0f }
    val z: Float get() = values.getOrElse(2) { 0f }
    val w: Float get() = values.getOrElse(3) { 0f }

    fun vector3(): Vec3 = Vec3(x, y, z)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SensorReading) return false
        return sensorId == other.sensorId &&
            kind == other.kind &&
            accuracy == other.accuracy &&
            timestampNanos == other.timestampNanos &&
            values.contentEquals(other.values)
    }

    override fun hashCode(): Int {
        var result = sensorId
        result = 31 * result + kind.hashCode()
        result = 31 * result + accuracy.hashCode()
        result = 31 * result + timestampNanos.hashCode()
        result = 31 * result + values.contentHashCode()
        return result
    }
}
