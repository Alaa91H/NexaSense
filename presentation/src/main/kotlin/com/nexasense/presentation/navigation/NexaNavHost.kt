package com.nexasense.presentation.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexasense.presentation.AppContainer
import com.nexasense.presentation.R
import com.nexasense.presentation.about.AboutScreen
import com.nexasense.presentation.compass.CompassScreen
import com.nexasense.presentation.level.LevelScreen
import com.nexasense.presentation.settings.SettingsScreen
import com.nexasense.presentation.theme.Motion

/**
 * The app's single navigation graph: three tools (Compass, Level, Settings)
 * switched via adaptive navigation, plus the About sub-screen.
 *
 * Navigation follows the window size class, not the device type:
 * - **Compact width** (<600dp, phones): bottom `NavigationBar`.
 * - **Medium width and up** (600dp+, tablets/foldables/desktop): a
 *   `NavigationRail` on the start side, the canonical M3 wide layout.
 *
 * Tabs follow the Google 2026 bottom-nav pattern: the selected tab shows the
 * filled Material Symbol, unselected tabs the outlined one, and switching
 * tabs cross-fades between the two icon variants and the screens themselves
 * on the M3 standard curves.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
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

    // Adaptive navigation: the rail replaces the bottom bar once the window
    // is medium width or wider (600dp+), independent of any device name. If
    // no Activity is found (should not happen in production), fall back to
    // the compact bottom bar.
    val windowSizeClass = activity?.let { calculateWindowSizeClass(it) }
    val useRail = windowSizeClass?.widthSizeClass != null &&
        windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    val tabs = remember {
        listOf(
            NavTab(Routes.COMPASS, R.drawable.ic_explore, R.drawable.ic_explore_filled, R.string.nav_compass),
            NavTab(Routes.LEVEL, R.drawable.ic_straighten, R.drawable.ic_straighten_filled, R.string.nav_level),
            NavTab(Routes.SETTINGS, R.drawable.ic_settings, R.drawable.ic_settings_filled, R.string.nav_settings),
        )
    }

    Row(modifier = Modifier.fillMaxSize()) {
        if (useRail) {
            NavigationRail {
                tabs.forEach { tab ->
                    NavigationRailItem(
                        selected = currentRoute == tab.route,
                        onClick = { navController.selectTab(tab.route) },
                        icon = {
                            TabIcon(
                                selected = currentRoute == tab.route,
                                outlined = painterResource(tab.outlined),
                                filled = painterResource(tab.filled),
                            )
                        },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
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
                if (!useRail) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = { navController.selectTab(tab.route) },
                                icon = {
                                    TabIcon(
                                        selected = currentRoute == tab.route,
                                        outlined = painterResource(tab.outlined),
                                        filled = painterResource(tab.filled),
                                    )
                                },
                                label = { Text(stringResource(tab.label)) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.COMPASS,
                modifier = Modifier.padding(innerPadding),
                // Google apps cross-fade between tabs; the outgoing screen fades
                // out on the standard accelerate curve while the incoming one
                // fades in on standard decelerate.
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = Motion.DurationMedium2,
                            easing = Motion.StandardDecelerate,
                        ),
                    )
                },
                exitTransition = {
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = Motion.DurationShort4,
                            easing = Motion.StandardAccelerate,
                        ),
                    )
                },
            ) {
                composable(Routes.COMPASS) {
                    CompassScreen(container = container)
                }
                composable(Routes.LEVEL) {
                    LevelScreen(container = container)
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        container = container,
                        onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    )
                }
                composable(Routes.ABOUT) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

/** A destination in the adaptive navigation (bottom bar or rail). */
private data class NavTab(
    val route: String,
    val outlined: Int,
    val filled: Int,
    val label: Int,
)

/**
 * Bottom-nav icon in the Google style: cross-fades between the outlined
 * Material Symbol (unselected) and the filled one (selected) on the M3
 * standard curve, instead of swapping abruptly. Used by both the bottom bar
 * and the rail so the two containers share the exact same icon behavior.
 */
@Composable
private fun TabIcon(selected: Boolean, outlined: Painter, filled: Painter) {
    Crossfade(
        targetState = selected,
        animationSpec = tween(
            durationMillis = Motion.DurationShort4,
            easing = Motion.StandardDecelerate,
        ),
        label = "tabIcon",
    ) { isSelected ->
        Icon(
            painter = if (isSelected) filled else outlined,
            contentDescription = null,
        )
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
