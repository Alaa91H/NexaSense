package com.nexasense.presentation.diagnostics

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexasense.presentation.AppContainer
import com.nexasense.presentation.R
import com.nexasense.presentation.components.DetailRow
import com.nexasense.presentation.components.GroupCard
import com.nexasense.presentation.components.ScreenScaffold
import com.nexasense.presentation.components.SectionHeader
import com.nexasense.presentation.components.StatusPill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val viewModel: DiagnosticsViewModel = viewModel(initializer = { DiagnosticsViewModel(container) })
    val state by viewModel.state.collectAsStateWithLifecycle()

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    ScreenScaffold(
        title = stringResource(R.string.diagnostics_title),
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            SectionHeader(text = stringResource(R.string.diagnostics_capabilities))
            GroupCard {
                state.capabilities.forEach { (feature, availability) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        StatusPill(
                            available = availability.isAvailable,
                            label = stringResource(
                                if (availability.isAvailable) R.string.available else R.string.not_available,
                            ),
                        )
                    }
                }
            }
            if (state.capabilities.any { !it.second.isAvailable }) {
                Text(
                    text = stringResource(R.string.diagnostics_missing_sensor),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            SectionHeader(text = stringResource(R.string.status))
            GroupCard {
                DetailRow(stringResource(R.string.diagnostics_sensor_count), state.sensors.size.toString())
                DetailRow(
                    stringResource(R.string.settings_developer_mode),
                    stringResource(if (state.developerMode) R.string.enabled else R.string.disabled),
                )
                DetailRow(
                    stringResource(R.string.calibrated),
                    stringResource(if (state.magnetometerCalibrated) R.string.calibrated else R.string.not_calibrated),
                )
            }

            SectionHeader(text = stringResource(R.string.diagnostics_crash_history))
            GroupCard {
                if (state.crashHistory.isEmpty()) {
                    Text(
                        text = stringResource(R.string.diagnostics_crash_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    state.crashHistory.forEach { crash ->
                        DetailRow(
                            label = formatCrashTime(crash.timestampMillis),
                            value = crash.throwableClassName,
                        )
                    }
                    OutlinedButton(
                        onClick = viewModel::clearCrashHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Text(stringResource(R.string.diagnostics_crash_clear))
                    }
                }
            }
            Text(
                text = stringResource(R.string.diagnostics_crash_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            if (state.developerMode) {
                SectionHeader(text = stringResource(R.string.diagnostics_developer_section))
                GroupCard {
                    Text(
                        text = stringResource(R.string.diagnostics_developer_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                    state.sensors.forEach { sensor ->
                        DetailRow(
                            label = sensor.kind.name,
                            value = "${sensor.vendor} · v${sensor.version} · type ${sensor.kind.type}",
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.diagnostics_no_personal_data),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        viewModel.generateReport()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.diagnostics_copy_report))
                }
                OutlinedButton(
                    onClick = {
                        state.reportText?.let { report ->
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(
                                Intent.createChooser(send, context.getString(R.string.diagnostics_share_report)),
                            )
                        }
                    },
                    enabled = state.reportText != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.diagnostics_share_report))
                }
            }

            // Copy the generated report to the clipboard as well.
            androidx.compose.runtime.LaunchedEffect(state.reportText) {
                state.reportText?.let { clipboard.setText(AnnotatedString(it)) }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatCrashTime(timestampMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestampMillis))
