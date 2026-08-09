package com.nexasense.core.logging

import android.util.Log
import com.nexasense.core.BuildConfig

/**
 * Central logging facade. Debug messages are stripped from release builds;
 * sensor value streams, locations and personal data are never logged.
 */
object NexaLogger {

    const val TAG = "NexaSense"

    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun w(message: String) {
        Log.w(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable == null) {
            Log.e(TAG, message)
        } else {
            Log.e(TAG, message, throwable)
        }
    }
}
