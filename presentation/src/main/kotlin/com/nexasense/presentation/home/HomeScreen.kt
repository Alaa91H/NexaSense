package com.nexasense.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexasense.presentation.AppContainer
import com.nexasense.presentation.components.NavigationRow
import com.nexasense.presentation.components.SectionHeader
import com.nexasense.presentation.components.StatusPill
import com.nexasense.presentation.navigation.Routes
import com.nexasense.presentation.R

@Composable
fun HomeScreen(
    container: AppContainer,
    onNavigate: (String) -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(initializer = { HomeViewModel(container) })
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.app_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.app_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        NavigationRow(
            icon = Icons.Outlined.Explore,
            title = stringResource(R.string.nav_compass),
            subtitle = stringResource(R.string.home_open_compass),
            onClick = { onNavigate(Routes.COMPASS) },
        )
        NavigationRow(
            icon = Icons.Outlined.Straighten,
            title = stringResource(R.string.nav_level),
            subtitle = stringResource(R.string.home_open_level),
            onClick = { onNavigate(Routes.LEVEL) },
        )
        NavigationRow(
            icon = Icons.Outlined.Memory,
            title = stringResource(R.string.nav_sensors),
            subtitle = stringResource(R.string.home_open_sensors),
            onClick = { onNavigate(Routes.SENSORS) },
        )
        NavigationRow(
            icon = Icons.Outlined.MonitorHeart,
            title = stringResource(R.string.nav_diagnostics),
            subtitle = stringResource(R.string.home_open_diagnostics),
            onClick = { onNavigate(Routes.DIAGNOSTICS) },
        )
        NavigationRow(
            icon = Icons.Outlined.Settings,
            title = stringResource(R.string.nav_settings),
            subtitle = stringResource(R.string.home_open_settings),
            onClick = { onNavigate(Routes.SETTINGS) },
        )
        NavigationRow(
            icon = Icons.Outlined.Info,
            title = stringResource(R.string.nav_about),
            subtitle = null,
            onClick = { onNavigate(Routes.ABOUT) },
        )

        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader(text = stringResource(R.string.status))
        FeatureStatusRow(
            label = stringResource(R.string.home_compass_status),
            available = state.compass,
        )
        FeatureStatusRow(
            label = stringResource(R.string.home_level_status),
            available = state.level,
        )
        FeatureStatusRow(
            label = stringResource(R.string.home_gyroscope_status),
            available = state.gyroscope,
        )
        FeatureStatusRow(
            label = stringResource(R.string.home_barometer_status),
            available = state.barometer,
        )
        FeatureStatusRow(
            label = stringResource(R.string.home_thermometer_status),
            available = state.thermometer,
        )
        FeatureStatusRow(
            label = stringResource(R.string.home_humidity_status),
            available = state.humidity,
        )
    }
}

@Composable
private fun FeatureStatusRow(label: String, available: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        StatusPill(
            available = available,
            label = stringResource(
                if (available) R.string.available else R.string.not_available,
            ),
        )
    }
}
