package com.nexasense.presentation.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexasense.presentation.AppContainer
import com.nexasense.presentation.R
import com.nexasense.presentation.compass.CompassScreen
import com.nexasense.presentation.level.LevelScreen
import com.nexasense.presentation.settings.SettingsScreen

/**
 * The app's single navigation graph: three tools (Compass, Level, Settings)
 * switched via a bottom navigation bar. The compass is the start destination
 * and acts as the home screen.
 */
@Composable
fun NexaNavHost(container: AppContainer) {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Only the compass is portrait-locked; the level and settings screens are
    // free to rotate with the device. Adjust the activity's requested
    // orientation whenever the destination changes.
    val activity = LocalContext.current.findActivity()
    DisposableEffect(currentRoute) {
        activity?.requestedOrientation = if (currentRoute == Routes.COMPASS) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {}
    }

    Scaffold(
        // Transparent so the theme's sky gradient shows behind the content.
        // IMPORTANT: a transparent container makes the default contentColor
        // resolve to BLACK (contentColorFor treats it as light), which turns
        // every implicit-color text invisible on the dark theme. Pin the
        // content color to the scheme's onSurface so readouts stay legible.
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Routes.COMPASS,
                    onClick = { navController.selectTab(Routes.COMPASS) },
                    icon = { Icon(Icons.Outlined.Explore, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_compass)) },
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.LEVEL,
                    onClick = { navController.selectTab(Routes.LEVEL) },
                    icon = { Icon(Icons.Outlined.Straighten, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_level)) },
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.SETTINGS,
                    onClick = { navController.selectTab(Routes.SETTINGS) },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings)) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.COMPASS,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.COMPASS) {
                CompassScreen(container = container)
            }
            composable(Routes.LEVEL) {
                LevelScreen(container = container)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(container = container)
            }
        }
    }
}

/** Walks context wrappers to the hosting [Activity], or null. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Switches to a tab, keeping a single instance and preserving state. */
private fun NavHostController.selectTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
