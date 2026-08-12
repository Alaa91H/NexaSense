package com.nexasense.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nexasense.presentation.R

/** Standard screen scaffold with an optional top bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Edge-to-edge is enforced from targetSdk 35: reserve the system
            // navigation bar so scrollable content never runs beneath it.
            // The TopAppBar already consumes the status-bar inset.
            .navigationBarsPadding(),
    ) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = null,
                        )
                    }
                }
            },
            actions = { actions() },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
        )
        content()
    }
}

/** Green/red availability pill. */
@Composable
fun StatusPill(
    available: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    val container = if (available) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val content = if (available) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        modifier = modifier,
    ) {
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            textAlign = TextAlign.Center,
        )
    }
}

/** Card container for groups of rows. */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

/**
 * Google-style leading icon: a Material Symbol inside a 40dp tonal rounded
 * container. Used by every settings row and expandable section header so the
 * whole app shares one icon treatment.
 */
@Composable
fun SettingsIcon(icon: Painter, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * The Google-settings row: optional leading icon in a tonal container, a
 * title, optional supporting text and an optional trailing slot. Tapping the
 * whole row triggers [onClick] when provided.
 */
@Composable
fun SettingsListItem(
    icon: Painter? = null,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    SettingsRowBase(
        icon = icon,
        title = title,
        subtitle = subtitle,
        trailing = trailing,
        interaction = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        modifier = modifier,
    )
}

/** A settings row with a switch: the whole row toggles (Google Settings style). */
@Composable
fun SettingsSwitchRow(
    icon: Painter? = null,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsRowBase(
        icon = icon,
        title = title,
        subtitle = subtitle,
        trailing = { Switch(checked = checked, onCheckedChange = null) },
        interaction = Modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        modifier = modifier,
    )
}

/** A settings row showing the current value plus a chevron; opens a picker. */
@Composable
fun SettingsValueRow(
    icon: Painter,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsListItem(
        icon = icon,
        title = title,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        onClick = onClick,
        modifier = modifier,
    )
}

/**
 * Google-style single-choice dialog: a scrollable radio list. Used by every
 * picker (Theme, Language, North reference, Smoothing, Sensor rate, Compass
 * style) so option selection behaves identically across the app.
 */
@Composable
fun <T> SettingsOptionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options.size) { index ->
                    val (value, label) = options[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = value == selected,
                                onClick = {
                                    onSelect(value)
                                    onDismiss()
                                },
                            )
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                        )
                        RadioButton(selected = value == selected, onClick = null)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/** Shared row layout behind the settings row variants. */
@Composable
private fun SettingsRowBase(
    icon: Painter?,
    title: String,
    subtitle: String?,
    trailing: (@Composable () -> Unit)?,
    interaction: Modifier,
    modifier: Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(interaction)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            SettingsIcon(icon)
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

/**
 * Keeps a sensor-backed component active only while the screen is in the
 * STARTED lifecycle state, unregistering sensors when it stops.
 */
@Composable
fun EngineLifecycleEffect(active: Boolean, onStateChanged: (Boolean) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, active) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> onStateChanged(true)
                Lifecycle.Event.ON_STOP -> onStateChanged(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onStateChanged(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose {
            onStateChanged(false)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
