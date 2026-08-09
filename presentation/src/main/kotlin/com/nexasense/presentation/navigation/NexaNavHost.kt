package com.nexasense.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexasense.presentation.AppContainer
import com.nexasense.presentation.about.AboutScreen
import com.nexasense.presentation.compass.CompassScreen
import com.nexasense.presentation.diagnostics.DiagnosticsScreen
import com.nexasense.presentation.home.HomeScreen
import com.nexasense.presentation.level.LevelScreen
import com.nexasense.presentation.sensordetail.SensorDetailScreen
import com.nexasense.presentation.sensors.SensorsScreen
import com.nexasense.presentation.settings.SettingsScreen

@Composable
fun NexaNavHost(container: AppContainer) {
    val navController: NavHostController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeScreen(container = container, onNavigate = navController::navigate)
        }
        composable(Routes.COMPASS) {
            CompassScreen(container = container, onBack = navController::navigateUp)
        }
        composable(Routes.LEVEL) {
            LevelScreen(container = container, onBack = navController::navigateUp)
        }
        composable(Routes.SENSORS) {
            SensorsScreen(container = container, onBack = navController::navigateUp, onOpen = { id ->
                navController.navigate(Routes.sensorDetail(id))
            })
        }
        composable(
            route = Routes.SENSOR_DETAIL,
            arguments = listOf(navArgument("sensorId") { type = NavType.IntType }),
        ) { entry ->
            val sensorId = entry.arguments?.getInt("sensorId") ?: -1
            SensorDetailScreen(
                sensorId = sensorId,
                container = container,
                onBack = navController::navigateUp,
            )
        }
        composable(Routes.DIAGNOSTICS) {
            DiagnosticsScreen(container = container, onBack = navController::navigateUp)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                container = container,
                onBack = navController::navigateUp,
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(container = container, onBack = navController::navigateUp)
        }
    }
}
