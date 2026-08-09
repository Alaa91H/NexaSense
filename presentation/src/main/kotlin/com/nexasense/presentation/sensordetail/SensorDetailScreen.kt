package com.nexasense.presentation.sensordetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexasense.domain.model.SensorReading
import com.nexasense.presentation.AppContainer
import com.nexasense.presentation.R
import com.nexasense.presentation.components.DetailRow
import com.nexasense.presentation.components.EngineLifecycleEffect
import com.nexasense.presentation.components.GroupCard
import com.nexasense.presentation.components.ScreenScaffold
import com.nexasense.presentation.components.SectionHeader
import com.nexasense.presentation.components.StatusPill
import java.util.Locale

@Composable
fun SensorDetailScreen(
    sensorId: Int,
    container: AppContainer,
    onBack: () -> Unit,
) {
    val viewModel: SensorDetailViewModel = viewModel(
        key = "sensor-$sensorId",
        initializer = {
            SensorDetailViewModel(
                sensorId = sensorId,
                sensorDiscovery = container.sensorDiscovery,
                streams = container.sensorEventStream,
                settingsStore = container.settingsStore,
            )
        },
    )
    val descriptor by viewModel.descriptor.collectAsStateWithLifecycle()
    val reading by viewModel.reading.collectAsStateWithLifecycle()
    val samplingStats by viewModel.samplingStats.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()

    EngineLifecycleEffect(active = isStreaming, onStateChanged = viewModel::setActive)

    val sensor = descriptor
    if (sensor == null) {
        ScreenScaffold(title = stringResource(R.string.sensor_detail_title), onBack = onBack) {
            Text(
                stringResource(R.string.sensors_empty),
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    ScreenScaffold(
        title = sensor.name,
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            GroupCard {
                DetailRow(stringResource(R.string.sensor_detail_name), sensor.name)
                DetailRow(stringResource(R.string.sensor_detail_vendor), sensor.vendor)
                DetailRow(stringResource(R.string.sensor_detail_version), sensor.version.toString())
                DetailRow(stringResource(R.string.sensor_detail_type), "${sensor.kind.name} (${sensor.kind.type})")
                DetailRow(stringResource(R.string.sensor_detail_string_type), sensor.stringType)
            }
            SectionHeader(text = stringResource(R.string.sensor_detail_sampling_rate))
            GroupCard {
                DetailRow(
                    stringResource(R.string.sensor_detail_requested),
                    samplingStats.requestedLabel,
                )
                DetailRow(
                    stringResource(R.string.sensor_detail_actual),
                    if (samplingStats.actualHz > 0f) {
                        stringResource(R.string.sensor_detail_actual_hz, format(samplingStats.actualHz))
                    } else {
                        stringResource(R.string.sensor_detail_waiting)
                    },
                )
                DetailRow(
                    stringResource(R.string.accuracy_label, accuracyText(reading)),
                    reading?.accuracy?.name ?: "—",
                )
                DetailRow(
                    stringResource(R.string.sensor_detail_timestamp),
                    reading?.timestampNanos?.toString() ?: "—",
                )
            }
            SectionHeader(text = stringResource(R.string.sensor_detail_current_value))
            GroupCard {
                if (sensor.isContinuous) {
                    RawValues(reading = reading)
                    DetailRow(
                        stringResource(R.string.sensor_detail_reporting_mode),
                        sensor.reportingMode,
                    )
                } else {
                    Text(
                        stringResource(R.string.sensor_detail_no_stream),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            SectionHeader(text = stringResource(R.string.status))
            GroupCard {
                DetailRow(stringResource(R.string.sensor_detail_resolution), format(sensor.resolution))
                DetailRow(stringResource(R.string.sensor_detail_max_range), format(sensor.maxRange))
                DetailRow(stringResource(R.string.sensor_detail_power), "${format(sensor.powerMilliAmps)} mA")
                DetailRow(stringResource(R.string.sensor_detail_min_delay), "${sensor.minDelayMicros} µs")
                DetailRow(stringResource(R.string.sensor_detail_max_delay), "${sensor.maxDelayMicros} µs")
                DetailRow(
                    stringResource(R.string.sensor_detail_wake_up),
                    stringResource(if (sensor.isWakeUp) R.string.sensor_detail_yes else R.string.sensor_detail_no),
                )
                DetailRow(
                    stringResource(R.string.sensor_detail_dynamic),
                    stringResource(if (sensor.isDynamic) R.string.sensor_detail_yes else R.string.sensor_detail_no),
                )
                DetailRow(stringResource(R.string.sensor_detail_max_fifo), sensor.maxFifoCount.toString())
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RawValues(reading: SensorReading?) {
    if (reading == null) {
        Text(
            stringResource(R.string.sensor_detail_waiting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
        return
    }
    val valueCount = reading.values.size
    val labels = listOf("X", "Y", "Z", "W")
    for (index in 0 until minOf(valueCount, 4)) {
        DetailRow(labels[index], format(reading.values[index]))
    }
    if (valueCount > 4) {
        DetailRow("…", valueCount.toString())
    }
}

private fun accuracyText(reading: SensorReading?): String = reading?.accuracy?.name ?: "—"

private fun format(value: Float): String = String.format(Locale.US, "%.6g", value)
