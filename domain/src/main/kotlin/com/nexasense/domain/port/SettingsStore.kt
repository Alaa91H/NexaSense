package com.nexasense.domain.port

import com.nexasense.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/** Persistence for user settings (implemented with DataStore). */
interface SettingsStore {
    val settings: Flow<AppSettings>

    suspend fun update(transform: (AppSettings) -> AppSettings)

    suspend fun reset()
}
