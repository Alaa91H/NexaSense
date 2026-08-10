package com.nexasense.core.crash

import com.nexasense.core.logging.NexaLogger
import com.nexasense.domain.port.CrashLogStore

/**
 * Records uncaught crashes to the local, offline [CrashLogStore] and then
 * delegates to the platform default handler so the crash still terminates the
 * app normally. Chaining to the previous handler is best practice — this
 * handler must never swallow the crash or mask it from the system.
 */
class CrashLogHandler(
    private val store: CrashLogStore,
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            store.record(buildText(thread, throwable))
            NexaLogger.e("Uncaught exception on ${thread.name}", throwable)
        } catch (t: Throwable) {
            // The handler itself must never throw.
            NexaLogger.e("Crash log handler failed", t)
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun buildText(thread: Thread, throwable: Throwable): String = buildString {
        appendLine("Thread: ${thread.name}")
        appendLine("Time: ${System.currentTimeMillis()}")
        appendLine()
        append(throwable.stackTraceToString())
    }
}
