package com.nexasense.presentation.navigation

/** Navigation routes. */
object Routes {
    const val HOME = "home"
    const val COMPASS = "compass"
    const val LEVEL = "level"
    const val SENSORS = "sensors"
    const val SENSOR_DETAIL = "sensor/{sensorId}"
    const val DIAGNOSTICS = "diagnostics"
    const val SETTINGS = "settings"
    const val ABOUT = "about"

    fun sensorDetail(sensorId: Int): String = "sensor/$sensorId"
}
