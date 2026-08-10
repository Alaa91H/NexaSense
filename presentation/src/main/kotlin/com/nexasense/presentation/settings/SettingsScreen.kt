package com.nexasense.presentation.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.nexasense.presentation.components.StatusPill

@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: (() -> Unit)? = null,
) {
    val viewModel: SettingsViewModel = viewModel(initializer = { SettingsViewModel(container) })
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val locationPermissionGranted by viewModel.locationPermissionGranted.collectAsStateWithLifecycle()

    var showResetDialog by remember { mutableStateOf(false) }
    // Accordion state: every section starts collapsed and only one is open at
    // a time — expanding a section collapses the previous one.
    var expandedSection by remember { mutableStateOf<String?>(null) }

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
        // LazyColumn with keyed items: expanding or collapsing a card never
        // makes the visible text jump — the scroll anchors to the first
        // visible item and keeps it in place as content above it changes.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = Section.THEME) {
                ExpandableSettingsSection(
                    title = stringResource(R.string.settings_theme),
                    expanded = expandedSection == Section.THEME,
                    onClick = { expandedSection = toggle(Section.THEME, expandedSection) },
                ) {
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
                }
            }

            item(key = Section.LANGUAGE) {
                ExpandableSettingsSection(
                    title = stringResource(R.string.settings_language),
                    expanded = expandedSection == Section.LANGUAGE,
                    onClick = { expandedSection = toggle(Section.LANGUAGE, expandedSection) },
                ) {
                    GroupCard {
                        RadioSetting(
                            options = LanguagePreference.entries.map { language ->
                                language to stringResource(languageLabel(language))
                            },
                            selected = settings.language,
                            onSelect = viewModel::setLanguage,
                        )
                    }
                }
            }

            item(key = Section.NORTH_REFERENCE) {
                ExpandableSettingsSection(
                    title = stringResource(R.string.settings_north_reference),
                    expanded = expandedSection == Section.NORTH_REFERENCE,
                    onClick = { expandedSection = toggle(Section.NORTH_REFERENCE, expandedSection) },
                ) {
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
                }
            }

            item(key = Section.QIBLA) {
                ExpandableSettingsSection(
                    title = stringResource(R.string.settings_qibla),
                    expanded = expandedSection == Section.QIBLA,
                    onClick = { expandedSection = toggle(Section.QIBLA, expandedSection) },
                ) {
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
                }
            }

            item(key = Section.SMOOTHING) {
                ExpandableSettingsSection(
                    title = stringResource(R.string.settings_smoothing),
                    expanded = expandedSection == Section.SMOOTHING,
                    onClick = { expandedSection = toggle(Section.SMOOTHING, expandedSection) },
                ) {
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
                }
            }

            item(key = Section.SENSOR_RATE) {
                ExpandableSettingsSection(
                    title = stringResource(R.string.settings_sensor_rate),
                    expanded = expandedSection == Section.SENSOR_RATE,
                    onClick = { expandedSection = toggle(Section.SENSOR_RATE, expandedSection) },
                ) {
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
                }
            }

            item(key = Section.COMPASS) {
                ExpandableSettingsSection(
                    title = stringResource(R.string.settings_compass),
                    expanded = expandedSection == Section.COMPASS,
                    onClick = { expandedSection = toggle(Section.COMPASS, expandedSection) },
                ) {
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
                        ToggleSetting(
                            label = stringResource(R.string.settings_compass_auto_hide_low_accuracy),
                            checked = settings.autoHideDetailsOnLowAccuracy,
                            onCheckedChange = viewModel::setAutoHideDetailsOnLowAccuracy,
                        )
                    }
                }
            }

            item(key = Section.LOCATION_PERMISSION) {
                ExpandableSettingsSection(
                    title = stringResource(R.string.settings_location_permission),
                    expanded = expandedSection == Section.LOCATION_PERMISSION,
                    onClick = { expandedSection = toggle(Section.LOCATION_PERMISSION, expandedSection) },
                ) {
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
                }
            }

            item(key = Section.STATUS) {
                ExpandableSettingsSection(
                    title = stringResource(R.string.status),
                    expanded = expandedSection == Section.STATUS,
                    onClick = { expandedSection = toggle(Section.STATUS, expandedSection) },
                ) {
                    GroupCard {
                        ToggleSetting(
                            label = stringResource(R.string.settings_haptics),
                            checked = settings.hapticsEnabled,
                            onCheckedChange = viewModel::setHaptics,
                        )
                        ToggleSetting(
                            label = stringResource(R.string.settings_level_sound),
                            checked = settings.levelSoundEnabled,
                            onCheckedChange = viewModel::setLevelSound,
                        )
                        ToggleSetting(
                            label = stringResource(R.string.settings_keep_screen_on),
                            checked = settings.keepScreenOn,
                            onCheckedChange = viewModel::setKeepScreenOn,
                        )
                    }
                }
            }

            item(key = "reset") {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(R.string.settings_reset))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
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

/** Stable section keys for the settings accordion. */
private object Section {
    const val THEME = "theme"
    const val LANGUAGE = "language"
    const val NORTH_REFERENCE = "north_reference"
    const val QIBLA = "qibla"
    const val SMOOTHING = "smoothing"
    const val SENSOR_RATE = "sensor_rate"
    const val COMPASS = "compass"
    const val LOCATION_PERMISSION = "location_permission"
    const val STATUS = "status"
}

/** Collapsed-by-default accordion: expanding a section collapses the previous one. */
private fun toggle(section: String, current: String?): String? =
    if (current == section) null else section

/** A tappable section header; the body is only composed while expanded. */
@Composable
private fun ExpandableSettingsSection(
    title: String,
    expanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        ExpandIndicator(expanded = expanded)
    }
    if (expanded) {
        content()
    }
}

/** A small chevron that points down when collapsed and up when expanded. */
@Composable
private fun ExpandIndicator(expanded: Boolean) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.size(24.dp)) {
        val strokeWidth = 2.dp.toPx()
        val w = size.width
        val h = size.height
        val path = Path().apply {
            if (expanded) {
                moveTo(w * 0.28f, h * 0.58f)
                lineTo(w * 0.5f, h * 0.38f)
                lineTo(w * 0.72f, h * 0.58f)
            } else {
                moveTo(w * 0.28f, h * 0.42f)
                lineTo(w * 0.5f, h * 0.62f)
                lineTo(w * 0.72f, h * 0.42f)
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
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
