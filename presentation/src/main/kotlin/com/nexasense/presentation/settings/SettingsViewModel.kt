package com.nexasense.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexasense.domain.model.AppSettings
import com.nexasense.domain.model.CompassStyle
import com.nexasense.domain.model.LanguagePreference
import com.nexasense.domain.model.NorthReference
import com.nexasense.domain.model.SensorRatePreference
import com.nexasense.domain.model.SmoothingPreference
import com.nexasense.domain.model.ThemePreference
import com.nexasense.presentation.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(container: AppContainer) : ViewModel() {

    private val settingsStore = container.settingsStore

    val settings: StateFlow<AppSettings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings.DEFAULT,
    )

    private val _locationPermissionGranted = MutableStateFlow(container.hasLocationPermission())
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted

    /** Re-reads the permission state (e.g. after returning from the dialog). */
    fun refreshLocationPermission(granted: Boolean) {
        _locationPermissionGranted.value = granted
    }

    fun setTheme(theme: ThemePreference) = update { it.copy(theme = theme) }

    fun setLanguage(language: LanguagePreference) = update { it.copy(language = language) }

    fun setNorthReference(reference: NorthReference) = update { it.copy(northReference = reference) }

    fun setQiblaEnabled(enabled: Boolean) = update { it.copy(qiblaEnabled = enabled) }

    fun setShowQiblaOnCompass(enabled: Boolean) = update { it.copy(showQiblaOnCompass = enabled) }

    fun setShowQiblaCard(enabled: Boolean) = update { it.copy(showQiblaCard = enabled) }

    fun setShowQiblaDistance(enabled: Boolean) = update { it.copy(showQiblaDistance = enabled) }

    fun setQiblaHapticFeedback(enabled: Boolean) = update { it.copy(qiblaHapticFeedback = enabled) }

    fun setSmoothing(smoothing: SmoothingPreference) = update { it.copy(smoothing = smoothing) }

    fun setSensorRate(rate: SensorRatePreference) = update { it.copy(sensorRate = rate) }

    fun setCompassStyle(style: CompassStyle) = update { it.copy(compassStyle = style) }

    fun setShowCardinalLabels(enabled: Boolean) = update { it.copy(showCardinalLabels = enabled) }

    fun setShowDegreeTicks(enabled: Boolean) = update { it.copy(showDegreeTicks = enabled) }

    fun setShowDegreeNumbers(enabled: Boolean) = update { it.copy(showDegreeNumbers = enabled) }

    fun setShowHeadingReadout(enabled: Boolean) = update { it.copy(showHeadingReadout = enabled) }

    fun setShowNorthReferenceBadge(enabled: Boolean) = update { it.copy(showNorthReferenceBadge = enabled) }

    fun setShowCompassDetails(enabled: Boolean) = update { it.copy(showCompassDetails = enabled) }

    fun setShowAccuracyPanel(enabled: Boolean) = update { it.copy(showAccuracyPanel = enabled) }

    fun setAutoHideDetailsOnLowAccuracy(enabled: Boolean) =
        update { it.copy(autoHideDetailsOnLowAccuracy = enabled) }

    fun setHaptics(enabled: Boolean) = update { it.copy(hapticsEnabled = enabled) }

    fun setKeepScreenOn(enabled: Boolean) = update { it.copy(keepScreenOn = enabled) }

    fun reset() {
        viewModelScope.launch { settingsStore.reset() }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsStore.update(transform) }
    }
}
