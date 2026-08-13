package com.nexasense.presentation.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nexasense.presentation.R
import com.nexasense.presentation.components.GroupCard
import com.nexasense.presentation.components.ScreenScaffold
import com.nexasense.presentation.components.SettingsDivider
import com.nexasense.presentation.components.SettingsListItem

/**
 * The Google Settings "About" screen: rows with Material Symbols in tonal
 * containers for the app version, the open-source/offline-first statements,
 * the sensor note and the bundled fonts' license. Reuses the same unified
 * row components as Settings so the whole app keeps one look.
 */
@Composable
fun AboutScreen(
    onBack: (() -> Unit)? = null,
) {
    // Version comes from the installed package at runtime, so it always
    // matches the APK actually running (no BuildConfig wiring in the
    // library module).
    val context = LocalContext.current
    val appInfo = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
    }
    val versionLabel = remember(appInfo) {
        appInfo?.let { "${it.versionName} (${it.versionCode})" } ?: ""
    }

    ScreenScaffold(
        title = stringResource(R.string.about_title),
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            GroupCard {
                SettingsListItem(
                    icon = painterResource(R.drawable.ic_verified),
                    title = stringResource(R.string.sensor_detail_version),
                    trailing = {
                        Text(
                            text = versionLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                SettingsDivider()
                SettingsListItem(
                    icon = painterResource(R.drawable.ic_code),
                    title = stringResource(R.string.about_open_source),
                )
                SettingsDivider()
                SettingsListItem(
                    icon = painterResource(R.drawable.ic_cloud_off),
                    title = stringResource(R.string.about_offline_first),
                )
            }

            GroupCard {
                SettingsListItem(
                    icon = painterResource(R.drawable.ic_sensors),
                    title = stringResource(R.string.about_sensors_note),
                )
                SettingsDivider()
                SettingsListItem(
                    icon = painterResource(R.drawable.ic_label),
                    title = stringResource(R.string.about_font_license),
                )
            }
        }
    }
}
