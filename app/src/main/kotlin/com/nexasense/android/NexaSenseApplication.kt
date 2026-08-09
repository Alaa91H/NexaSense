package com.nexasense.android

import android.app.Application
import com.nexasense.presentation.AppContainer

class NexaSenseApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)
    }
}
