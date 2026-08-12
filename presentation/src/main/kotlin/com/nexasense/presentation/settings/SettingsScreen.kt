package com.nexasense.presentation.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
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
import com.nexasense.presentation.components.SettingsIcon
import com.nexasense.presentation.components.SettingsListItem
import com.nexasense.presentation.components.SettingsOptionDialog
import com.nexasense.presentation.components.SettingsSwitchRow
import com.nexasense.presentation.components.SettingsValueRow
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
    // Which single-choice picker is open (null = none). Options open as
    // Google-style dialogs instead of long inline lists.
    var picker by remember { mutableStateOf<Picker?>(null) }
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
            item(key = "general_label") {
                SettingsSectionLabel(stringResource(R.string.settings_section_general))
            }

            item(key = "general") {
                GroupCard {
                    SettingsValueRow(
                        icon = painterResource(R.drawable.ic_palette),
                        title = stringResource(R.string.settings_theme),
                        value = stringResource(themeLabel(settings.theme)),
                        onClick = { picker = Picker.THEME },
                    )
                    SettingsDivider()
                    SettingsValueRow(
                        icon = painterResource(R.drawable.ic_translate),
                        title = stringResource(R.string.settings_language),
                        value = stringResource(languageLabel(settings.language)),
                        onClick = { picker = Picker.LANGUAGE },
                    )
                }
            }

            item(key = Section.QIBLA) {
                ExpandableSettingsSection(
                    icon = painterResource(R.drawable.ic_mosque),
                    title = stringResource(R.string.settings_qibla),
                    expanded = expandedSection == Section.QIBLA,
                    onClick = { expandedSection = toggle(Section.QIBLA, expandedSection) },
                ) {
                    GroupCard {
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_qibla_enable),
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
                            SettingsDivider()
                            SettingsSwitchRow(
                                title = stringResource(R.string.settings_qibla_show_on_compass),
                                checked = settings.showQiblaOnCompass,
                                onCheckedChange = viewModel::setShowQiblaOnCompass,
                            )
                            SettingsDivider()
                            SettingsSwitchRow(
                                title = stringResource(R.string.settings_qibla_show_card),
                                checked = settings.showQiblaCard,
                                onCheckedChange = viewModel::setShowQiblaCard,
                            )
                            SettingsDivider()
                            SettingsSwitchRow(
                                title = stringResource(R.string.settings_qibla_show_distance),
                                checked = settings.showQiblaDistance,
                                onCheckedChange = viewModel::setShowQiblaDistance,
                            )
                            SettingsDivider()
                            SettingsSwitchRow(
                                title = stringResource(R.string.settings_qibla_haptic),
                                checked = settings.qiblaHapticFeedback,
                                onCheckedChange = viewModel::setQiblaHapticFeedback,
                            )
                        }
                    }
                    SettingsDescription(stringResource(R.string.settings_qibla_enable_desc))
                }
            }

            item(key = Section.COMPASS) {
                ExpandableSettingsSection(
                    icon = painterResource(R.drawable.ic_compass_calibration),
                    title = stringResource(R.string.settings_compass),
                    expanded = expandedSection == Section.COMPASS,
                    onClick = { expandedSection = toggle(Section.COMPASS, expandedSection) },
                ) {
                    GroupCard {
                        SettingsValueRow(
                            icon = painterResource(R.drawable.ic_explore),
                            title = stringResource(R.string.settings_compass_style),
                            value = stringResource(compassStyleLabel(settings.compassStyle)),
                            onClick = { picker = Picker.COMPASS_STYLE },
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_compass_show_cardinal_labels),
                            checked = settings.showCardinalLabels,
                            onCheckedChange = viewModel::setShowCardinalLabels,
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_compass_show_ticks),
                            checked = settings.showDegreeTicks,
                            onCheckedChange = viewModel::setShowDegreeTicks,
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_compass_show_degree_numbers),
                            checked = settings.showDegreeNumbers,
                            onCheckedChange = viewModel::setShowDegreeNumbers,
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_compass_show_heading_readout),
                            checked = settings.showHeadingReadout,
                            onCheckedChange = viewModel::setShowHeadingReadout,
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_compass_show_north_reference_badge),
                            checked = settings.showNorthReferenceBadge,
                            onCheckedChange = viewModel::setShowNorthReferenceBadge,
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_compass_show_details),
                            checked = settings.showCompassDetails,
                            onCheckedChange = viewModel::setShowCompassDetails,
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_compass_show_accuracy_panel),
                            checked = settings.showAccuracyPanel,
                            onCheckedChange = viewModel::setShowAccuracyPanel,
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_compass_auto_hide_low_accuracy),
                            checked = settings.autoHideDetailsOnLowAccuracy,
                            onCheckedChange = viewModel::setAutoHideDetailsOnLowAccuracy,
                        )
                    }
                    SettingsDescription(stringResource(R.string.settings_compass_style_desc))
                }
            }

            item(key = "compass_options") {
                GroupCard {
                    SettingsValueRow(
                        icon = painterResource(R.drawable.ic_north_east),
                        title = stringResource(R.string.settings_north_reference),
                        value = stringResource(northReferenceLabel(settings.northReference)),
                        onClick = { picker = Picker.NORTH_REFERENCE },
                    )
                    SettingsDivider()
                    SettingsValueRow(
                        icon = painterResource(R.drawable.ic_tune),
                        title = stringResource(R.string.settings_smoothing),
                        value = stringResource(smoothingLabel(settings.smoothing)),
                        onClick = { picker = Picker.SMOOTHING },
                    )
                    SettingsDivider()
                    SettingsValueRow(
                        icon = painterResource(R.drawable.ic_sensors),
                        title = stringResource(R.string.settings_sensor_rate),
                        value = stringResource(sensorRateLabel(settings.sensorRate)),
                        onClick = { picker = Picker.SENSOR_RATE },
                    )
                }
            }

            item(key = Section.LOCATION_PERMISSION) {
                ExpandableSettingsSection(
                    icon = painterResource(R.drawable.ic_location_on),
                    title = stringResource(R.string.settings_location_permission),
                    expanded = expandedSection == Section.LOCATION_PERMISSION,
                    onClick = { expandedSection = toggle(Section.LOCATION_PERMISSION, expandedSection) },
                ) {
                    GroupCard {
                        SettingsListItem(
                            title = stringResource(R.string.settings_location_permission),
                            trailing = {
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
                            },
                        )
                    }
                    SettingsDescription(stringResource(R.string.settings_location_permission_note))
                }
            }

            item(key = Section.STATUS) {
                ExpandableSettingsSection(
                    icon = painterResource(R.drawable.ic_vibration),
                    title = stringResource(R.string.status),
                    expanded = expandedSection == Section.STATUS,
                    onClick = { expandedSection = toggle(Section.STATUS, expandedSection) },
                ) {
                    GroupCard {
                        SettingsSwitchRow(
                            icon = painterResource(R.drawable.ic_vibration),
                            title = stringResource(R.string.settings_haptics),
                            checked = settings.hapticsEnabled,
                            onCheckedChange = viewModel::setHaptics,
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            icon = painterResource(R.drawable.ic_volume_up),
                            title = stringResource(R.string.settings_level_sound),
                            checked = settings.levelSoundEnabled,
                            onCheckedChange = viewModel::setLevelSound,
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            icon = painterResource(R.drawable.ic_screen_lock_portrait),
                            title = stringResource(R.string.settings_keep_screen_on),
                            checked = settings.keepScreenOn,
                            onCheckedChange = viewModel::setKeepScreenOn,
                        )
                    }
                }
            }

            item(key = "reset") {
                Spacer(modifier = Modifier.height(12.dp))
                GroupCard {
                    SettingsListItem(
                        icon = painterResource(R.drawable.ic_restart_alt),
                        title = stringResource(R.string.settings_reset),
                        onClick = { showResetDialog = true },
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    when (picker) {
        Picker.THEME -> SettingsOptionDialog(
            title = stringResource(R.string.settings_theme),
            options = listOf(
                ThemePreference.SYSTEM to stringResource(R.string.settings_theme_system),
                ThemePreference.LIGHT to stringResource(R.string.settings_theme_light),
                ThemePreference.DARK to stringResource(R.string.settings_theme_dark),
            ),
            selected = settings.theme,
            onSelect = viewModel::setTheme,
            onDismiss = { picker = null },
        )

        Picker.LANGUAGE -> SettingsOptionDialog(
            title = stringResource(R.string.settings_language),
            options = LanguagePreference.entries.map { language ->
                language to stringResource(languageLabel(language))
            },
            selected = settings.language,
            onSelect = viewModel::setLanguage,
            onDismiss = { picker = null },
        )

        Picker.NORTH_REFERENCE -> SettingsOptionDialog(
            title = stringResource(R.string.settings_north_reference),
            options = listOf(
                NorthReference.AUTOMATIC to stringResource(R.string.settings_north_reference_automatic),
                NorthReference.TRUE_NORTH to stringResource(R.string.settings_north_reference_true),
                NorthReference.MAGNETIC_NORTH to stringResource(R.string.settings_north_reference_magnetic),
            ),
            selected = settings.northReference,
            onSelect = viewModel::setNorthReference,
            onDismiss = { picker = null },
        )

        Picker.SMOOTHING -> SettingsOptionDialog(
            title = stringResource(R.string.settings_smoothing),
            options = listOf(
                SmoothingPreference.NONE to stringResource(R.string.settings_smoothing_none),
                SmoothingPreference.LIGHT to stringResource(R.string.settings_smoothing_light),
                SmoothingPreference.MEDIUM to stringResource(R.string.settings_smoothing_medium),
                SmoothingPreference.STRONG to stringResource(R.string.settings_smoothing_strong),
            ),
            selected = settings.smoothing,
            onSelect = viewModel::setSmoothing,
            onDismiss = { picker = null },
        )

        Picker.SENSOR_RATE -> SettingsOptionDialog(
            title = stringResource(R.string.settings_sensor_rate),
            options = listOf(
                SensorRatePreference.NORMAL to stringResource(R.string.settings_rate_normal),
                SensorRatePreference.UI to stringResource(R.string.settings_rate_ui),
                SensorRatePreference.GAME to stringResource(R.string.settings_rate_game),
            ),
            selected = settings.sensorRate,
            onSelect = viewModel::setSensorRate,
            onDismiss = { picker = null },
        )

        Picker.COMPASS_STYLE -> SettingsOptionDialog(
            title = stringResource(R.string.settings_compass_style),
            options = listOf(
                CompassStyle.CLASSIC to stringResource(R.string.settings_compass_style_classic),
                CompassStyle.AZIMUTH to stringResource(R.string.settings_compass_style_azimuth),
                CompassStyle.MINIMAL to stringResource(R.string.settings_compass_style_minimal),
            ),
            selected = settings.compassStyle,
            onSelect = viewModel::setCompassStyle,
            onDismiss = { picker = null },
        )

        null -> Unit
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

/** Single-choice pickers opened from the settings rows. */
private enum class Picker {
    THEME,
    LANGUAGE,
    NORTH_REFERENCE,
    SMOOTHING,
    SENSOR_RATE,
    COMPASS_STYLE,
}

/** Stable section keys for the settings accordion. */
private object Section {
    const val QIBLA = "qibla"
    const val COMPASS = "compass"
    const val LOCATION_PERMISSION = "location_permission"
    const val STATUS = "status"
}

/** Collapsed-by-default accordion: expanding a section collapses the previous one. */
private fun toggle(section: String, current: String?): String? =
    if (current == section) null else section

/** Small primary label above a settings group (Google Settings style). */
@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

/** Subtle divider between rows inside a settings group. */
@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

/** Supporting text below a settings group. */
@Composable
private fun SettingsDescription(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
    )
}

/**
 * A tappable section header whose body animates open/closed when toggled.
 * The size change animates smoothly and is clipped to the growing bounds
 * (animateContentSize + clipToBounds), so the revealed content never
 * overlaps the rows below it — unlike a raw AnimatedVisibility inside a
 * LazyColumn item, which draws the full content beyond the animating
 * bounds. The LazyColumn's scroll anchoring keeps the visible text fixed
 * while the card expands or collapses.
 */
@Composable
private fun ExpandableSettingsSection(
    icon: Painter,
    title: String,
    expanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .animateContentSize(animationSpec = tween(durationMillis = 220))
            .clipToBounds(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIcon(icon)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            ExpandIndicator(expanded = expanded)
        }
        if (expanded) {
            content()
        }
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

private fun themeLabel(theme: ThemePreference): Int = when (theme) {
    ThemePreference.SYSTEM -> R.string.settings_theme_system
    ThemePreference.LIGHT -> R.string.settings_theme_light
    ThemePreference.DARK -> R.string.settings_theme_dark
}

private fun northReferenceLabel(reference: NorthReference): Int = when (reference) {
    NorthReference.AUTOMATIC -> R.string.settings_north_reference_automatic
    NorthReference.TRUE_NORTH -> R.string.settings_north_reference_true
    NorthReference.MAGNETIC_NORTH -> R.string.settings_north_reference_magnetic
}

private fun smoothingLabel(smoothing: SmoothingPreference): Int = when (smoothing) {
    SmoothingPreference.NONE -> R.string.settings_smoothing_none
    SmoothingPreference.LIGHT -> R.string.settings_smoothing_light
    SmoothingPreference.MEDIUM -> R.string.settings_smoothing_medium
    SmoothingPreference.STRONG -> R.string.settings_smoothing_strong
}

private fun sensorRateLabel(rate: SensorRatePreference): Int = when (rate) {
    SensorRatePreference.NORMAL -> R.string.settings_rate_normal
    SensorRatePreference.UI -> R.string.settings_rate_ui
    SensorRatePreference.GAME -> R.string.settings_rate_game
}

private fun compassStyleLabel(style: CompassStyle): Int = when (style) {
    CompassStyle.CLASSIC -> R.string.settings_compass_style_classic
    CompassStyle.AZIMUTH -> R.string.settings_compass_style_azimuth
    CompassStyle.MINIMAL -> R.string.settings_compass_style_minimal
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
