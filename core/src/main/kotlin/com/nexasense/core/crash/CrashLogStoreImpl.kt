package com.nexasense.core.crash

import android.content.Context
import com.nexasense.core.logging.NexaLogger
import com.nexasense.domain.port.CrashLogStore
import com.nexasense.domain.port.CrashRecord
import java.io.File

/**
 * File-based crash history in the app's private storage (`filesDir`). Offline
 * by design: the app declares no INTERNET permission, so crash details can
 * never leave the device. Keeps only the most recent [maxRecords] crashes so
 * the history stays small and bounded.
 */
class CrashLogStoreImpl(
    context: Context,
    private val maxRecords: Int = 10,
) : CrashLogStore {

    private val dir = File(context.filesDir, "crash_logs").apply { mkdirs() }

    override val crashes: List<CrashRecord>
        get() = synchronized(this) {
            dir.listFiles { f -> f.isFile && f.extension == "txt" }
                ?.sortedByDescending { it.lastModified() }
                ?.mapNotNull { readRecord(it) }
                ?.toList()
                ?: emptyList()
        }

    override fun record(crashText: String) {
        synchronized(this) {
            trimTo(maxRecords - 1)
            val file = File(dir, "crash_${System.currentTimeMillis()}.txt")
            runCatching { file.writeText(crashText) }
                .onFailure { NexaLogger.e("Failed to write crash log", it) }
        }
    }

    override fun clear() {
        synchronized(this) {
            dir.listFiles()?.forEach { runCatching { it.delete() } }
        }
    }

    private fun trimTo(keep: Int) {
        if (keep < 0) return
        dir.listFiles { f -> f.isFile && f.extension == "txt" }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(keep)
            ?.forEach { runCatching { it.delete() } }
    }

    private fun readRecord(file: File): CrashRecord? = runCatching {
        val text = file.readText()
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        CrashRecord(
            timestampMillis = file.name.removePrefix("crash_").removeSuffix(".txt")
                .toLongOrNull() ?: file.lastModified(),
            throwableClassName = firstLine.substringBefore(":").trim(),
            message = firstLine.substringAfter(":", "").trim().takeIf { it.isNotEmpty() },
            stackTrace = text,
        )
    }.getOrNull()
}
