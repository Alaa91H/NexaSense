package com.nexasense.android

import android.app.Application
import com.nexasense.core.crash.CrashLogHandler
import com.nexasense.core.crash.CrashLogStoreImpl
import com.nexasense.presentation.AppContainer

class NexaSenseApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Local, offline crash logging — the app has no INTERNET permission, so
        // crash details are stored only on this device and shown in the
        // Diagnostics screen. Installed before the container is built so even
        // a crash during dependency construction is captured.
        val crashLogStore = CrashLogStoreImpl(this)
        Thread.setDefaultUncaughtExceptionHandler(CrashLogHandler(crashLogStore))
        container = AppContainerImpl(this, crashLogStore)
    }
}
