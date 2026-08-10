package com.nexasense.presentation.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexasense.domain.model.CompassStyle
import com.nexasense.domain.model.LanguagePreference
import com.nexasense.domain.model.NorthReference
import com.nexasense.domain.model.SensorRatePreference
import com.nexasense.domain.model.SmoothingPreference
import com.nexasense.domain.model.ThemePreference
import com.nexasense.presentation.AppContainer
import com.nexasense.presentation.R
import com.nexasense.presentation.components.GroupCard
import com.nexasense.presentation.components.ScreenScaffold
import com.nexasense.presentation.components.SectionHeader
import com.nexasense.presentation.components.StatusPill

@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit = {},
) {
    val viewModel: SettingsViewModel = viewModel(initializer = { SettingsViewModel(container) })
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val locationPermissionGranted by viewModel.locationPermissionGranted.collectAsStateWithLifecycle()

    var showResetDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.refreshLocationPermission(granted)
        // Only enable Qibla once the permission is actually granted.
        if (!granted) viewModel.setQiblaEnabled(false)
    }

    ScreenScaffold(
        title = stringResource(R.string.settings_title),
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            SectionHeader(text = stringResource(R.string.settings_theme))
            GroupCard {
                RadioSetting(
                    options = listOf(
                        ThemePreference.SYSTEM to stringResource(R.string.settings_theme_system),
                        ThemePreference.LIGHT to stringResource(R.string.settings_theme_light),
                        ThemePreference.DARK to stringResource(R.string.settings_theme_dark),
                    ),
                    selected = settings.theme,
                    onSelect = viewModel::setTheme,
                )
            }

            SectionHeader(text = stringResource(R.string.settings_language))
            GroupCard {
                RadioSetting(
                    options = LanguagePreference.entries.map { language ->
                        language to stringResource(languageLabel(language))
                    },
                    selected = settings.language,
                    onSelect = viewModel::setLanguage,
                )
            }

            SectionHeader(text = stringResource(R.string.settings_north_reference))
            GroupCard {
                RadioSetting(
                    options = listOf(
                        NorthReference.AUTOMATIC to stringResource(R.string.settings_north_reference_automatic),
                        NorthReference.TRUE_NORTH to stringResource(R.string.settings_north_reference_true),
                        NorthReference.MAGNETIC_NORTH to stringResource(R.string.settings_north_reference_magnetic),
                    ),
                    selected = settings.northReference,
                    onSelect = viewModel::setNorthReference,
                )
            }
            Text(
                text = when (settings.northReference) {
                    NorthReference.AUTOMATIC -> stringResource(R.string.settings_north_reference_automatic_desc)
                    NorthReference.TRUE_NORTH -> stringResource(R.string.settings_north_reference_true_desc)
                    NorthReference.MAGNETIC_NORTH -> stringResource(R.string.settings_north_reference_magnetic)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            SectionHeader(text = stringResource(R.string.settings_qibla))
            GroupCard {
                ToggleSetting(
                    label = stringResource(R.string.settings_qibla_enable),
                    checked = settings.qiblaEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !locationPermissionGranted) {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        } else {
                            viewModel.setQiblaEnabled(enabled)
                        }
                    },
                )
                if (settings.qiblaEnabled) {
                    ToggleSetting(
                        label = stringResource(R.string.settings_qibla_show_on_compass),
                        checked = settings.showQiblaOnCompass,
                        onCheckedChange = viewModel::setShowQiblaOnCompass,
                    )
                    ToggleSetting(
                        label = stringResource(R.string.settings_qibla_show_card),
                        checked = settings.showQiblaCard,
                        onCheckedChange = viewModel::setShowQiblaCard,
                    )
                    ToggleSetting(
                        label = stringResource(R.string.settings_qibla_show_distance),
                        checked = settings.showQiblaDistance,
                        onCheckedChange = viewModel::setShowQiblaDistance,
                    )
                    ToggleSetting(
                        label = stringResource(R.string.settings_qibla_haptic),
                        checked = settings.qiblaHapticFeedback,
                        onCheckedChange = viewModel::setQiblaHapticFeedback,
                    )
                }
            }
            Text(
                text = stringResource(R.string.settings_qibla_enable_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            SectionHeader(text = stringResource(R.string.settings_smoothing))
            GroupCard {
                RadioSetting(
                    options = listOf(
                        SmoothingPreference.NONE to stringResource(R.string.settings_smoothing_none),
                        SmoothingPreference.LIGHT to stringResource(R.string.settings_smoothing_light),
                        SmoothingPreference.MEDIUM to stringResource(R.string.settings_smoothing_medium),
                        SmoothingPreference.STRONG to stringResource(R.string.settings_smoothing_strong),
                    ),
                    selected = settings.smoothing,
                    onSelect = viewModel::setSmoothing,
                )
            }

            SectionHeader(text = stringResource(R.string.settings_sensor_rate))
            GroupCard {
                RadioSetting(
                    options = listOf(
                        SensorRatePreference.NORMAL to stringResource(R.string.settings_rate_normal),
                        SensorRatePreference.UI to stringResource(R.string.settings_rate_ui),
                        SensorRatePreference.GAME to stringResource(R.string.settings_rate_game),
                    ),
                    selected = settings.sensorRate,
                    onSelect = viewModel::setSensorRate,
                )
            }

            SectionHeader(text = stringResource(R.string.settings_compass))
            GroupCard {
                Text(
                    text = stringResource(R.string.settings_compass_style),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                RadioSetting(
                    options = listOf(
                        CompassStyle.CLASSIC to stringResource(R.string.settings_compass_style_classic),
                        CompassStyle.AZIMUTH to stringResource(R.string.settings_compass_style_azimuth),
                        CompassStyle.MINIMAL to stringResource(R.string.settings_compass_style_minimal),
                    ),
                    selected = settings.compassStyle,
                    onSelect = viewModel::setCompassStyle,
                )
            }
            Text(
                text = stringResource(R.string.settings_compass_style_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            GroupCard {
                ToggleSetting(
                    label = stringResource(R.string.settings_compass_show_cardinal_labels),
                    checked = settings.showCardinalLabels,
                    onCheckedChange = viewModel::setShowCardinalLabels,
                )
                ToggleSetting(
                    label = stringResource(R.string.settings_compass_show_ticks),
                    checked = settings.showDegreeTicks,
                    onCheckedChange = viewModel::setShowDegreeTicks,
                )
                ToggleSetting(
                    label = stringResource(R.string.settings_compass_show_degree_numbers),
                    checked = settings.showDegreeNumbers,
                    onCheckedChange = viewModel::setShowDegreeNumbers,
                )
                ToggleSetting(
                    label = stringResource(R.string.settings_compass_show_heading_readout),
                    checked = settings.showHeadingReadout,
                    onCheckedChange = viewModel::setShowHeadingReadout,
                )
                ToggleSetting(
                    label = stringResource(R.string.settings_compass_show_north_reference_badge),
                    checked = settings.showNorthReferenceBadge,
                    onCheckedChange = viewModel::setShowNorthReferenceBadge,
                )
                ToggleSetting(
                    label = stringResource(R.string.settings_compass_show_details),
                    checked = settings.showCompassDetails,
                    onCheckedChange = viewModel::setShowCompassDetails,
                )
                ToggleSetting(
                    label = stringResource(R.string.settings_compass_show_accuracy_panel),
                    checked = settings.showAccuracyPanel,
                    onCheckedChange = viewModel::setShowAccuracyPanel,
                )
            }

            SectionHeader(text = stringResource(R.string.settings_location_permission))
            GroupCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_location_permission),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    StatusPill(
                        available = locationPermissionGranted,
                        label = stringResource(
                            if (locationPermissionGranted) {
                                R.string.settings_location_permission_granted
                            } else {
                                R.string.settings_location_permission_denied
                            },
                        ),
                    )
                }
            }
            Text(
                text = stringResource(R.string.settings_location_permission_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            SectionHeader(text = stringResource(R.string.status))
            GroupCard {
                ToggleSetting(
                    label = stringResource(R.string.settings_haptics),
                    checked = settings.hapticsEnabled,
                    onCheckedChange = viewModel::setHaptics,
                )
                ToggleSetting(
                    label = stringResource(R.string.settings_keep_screen_on),
                    checked = settings.keepScreenOn,
                    onCheckedChange = viewModel::setKeepScreenOn,
                )
                ToggleSetting(
                    label = stringResource(R.string.settings_developer_mode),
                    checked = settings.developerMode,
                    onCheckedChange = viewModel::setDeveloperMode,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(stringResource(R.string.settings_reset))
            }
            TextButton(
                onClick = onOpenAbout,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(stringResource(R.string.nav_about))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.settings_reset)) },
            text = { Text(stringResource(R.string.settings_reset_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.reset()
                        showResetDialog = false
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private fun languageLabel(language: LanguagePreference): Int = when (language) {
    LanguagePreference.SYSTEM -> R.string.settings_language_system
    LanguagePreference.ENGLISH -> R.string.settings_language_english
    LanguagePreference.ARABIC -> R.string.settings_language_arabic
    LanguagePreference.GERMAN -> R.string.settings_language_german
    LanguagePreference.FRENCH -> R.string.settings_language_french
    LanguagePreference.SPANISH -> R.string.settings_language_spanish
    LanguagePreference.PORTUGUESE -> R.string.settings_language_portuguese
    LanguagePreference.ITALIAN -> R.string.settings_language_italian
    LanguagePreference.TURKISH -> R.string.settings_language_turkish
    LanguagePreference.RUSSIAN -> R.string.settings_language_russian
    LanguagePreference.UKRAINIAN -> R.string.settings_language_ukrainian
    LanguagePreference.POLISH -> R.string.settings_language_polish
    LanguagePreference.DUTCH -> R.string.settings_language_dutch
    LanguagePreference.INDONESIAN -> R.string.settings_language_indonesian
    LanguagePreference.MALAY -> R.string.settings_language_malay
    LanguagePreference.HINDI -> R.string.settings_language_hindi
    LanguagePreference.BENGALI -> R.string.settings_language_bengali
    LanguagePreference.URDU -> R.string.settings_language_urdu
    LanguagePreference.PERSIAN -> R.string.settings_language_persian
    LanguagePreference.CHINESE_SIMPLIFIED -> R.string.settings_language_chinese_simplified
    LanguagePreference.CHINESE_TRADITIONAL -> R.string.settings_language_chinese_traditional
    LanguagePreference.JAPANESE -> R.string.settings_language_japanese
    LanguagePreference.KOREAN -> R.string.settings_language_korean
    LanguagePreference.VIETNAMESE -> R.string.settings_language_vietnamese
    LanguagePreference.THAI -> R.string.settings_language_thai
}

@Composable
private fun <T> RadioSetting(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    options.forEach { (value, label) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                )
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = value == selected, onClick = { onSelect(value) })
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun ToggleSetting(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = androidx.compose.ui.semantics.Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}
