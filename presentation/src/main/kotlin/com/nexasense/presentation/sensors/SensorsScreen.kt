package com.nexasense.presentation.sensors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.nexasense.domain.model.SensorDescriptor
import com.nexasense.presentation.AppContainer
import com.nexasense.presentation.R
import com.nexasense.presentation.components.ScreenScaffold
import com.nexasense.presentation.components.StatusPill

@Composable
fun SensorsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpen: (Int) -> Unit,
) {
    val viewModel: SensorsViewModel = viewModel(initializer = { SensorsViewModel(container) })
    val sensors by viewModel.sensors.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = stringResource(R.string.sensors_title),
        onBack = onBack,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 24.dp,
            ),
        ) {
            item {
                Text(
                    text = stringResource(R.string.sensors_count, sensors.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (sensors.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.sensors_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            items(sensors, key = { it.id }) { sensor ->
                SensorRow(sensor = sensor, onClick = { onOpen(sensor.id) })
            }
        }
    }
}

@Composable
private fun SensorRow(sensor: SensorDescriptor, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensor.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${sensor.kind.name} · ${stringResource(R.string.sensor_type_id, sensor.kind.type)} · ${sensor.vendor}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(
                available = sensor.isContinuous,
                label = stringResource(
                    if (sensor.isContinuous) R.string.sensor_continuous else R.string.sensor_one_shot,
                ),
            )
        }
    }
}
