package com.nexasense.domain.port

/** A recorded app crash, stored only on the device. */
data class CrashRecord(
    val timestampMillis: Long,
    val throwableClassName: String,
    val message: String?,
    val stackTrace: String,
)

/**
 * Local, offline crash history. The app has no INTERNET permission, so crash
 * details can never leave the device — this is privacy-first by construction
 * and lets users/developers diagnose problems (e.g. on the Diagnostics
 * screen) without any network.
 */
interface CrashLogStore {
    /** Most recent crashes first. */
    val crashes: List<CrashRecord>

    /** Persists a formatted crash report (called from any thread). */
    fun record(crashText: String)

    /** Deletes all recorded crashes. */
    fun clear()
}
