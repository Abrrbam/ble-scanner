package com.goodeva.blescannertracker.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.goodeva.blescannertracker.ui.history.HistoryScreen
import com.goodeva.blescannertracker.ui.radar.RadarScreen
import com.goodeva.blescannertracker.ui.scanner.ScannerScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SCANNER,
        modifier = modifier
    ) {
        composable(Routes.SCANNER) {
            ScannerScreen(
                onDeviceClick = { address -> navController.navigate(Routes.radar(address))},
                onHistoryClick = { navController.navigate(Routes.HISTORY)},
            )
        }
        composable(
            route = Routes.RADAR,
            arguments = listOf(navArgument(Routes.ARG_ADDRESS) { type = NavType.StringType})
        ) { entry ->
            val address = Uri.decode(entry.arguments?. getString(Routes.ARG_ADDRESS).orEmpty())
            RadarScreen(address = address, onBack = { navController.popBackStack()})

        }
        composable(Routes.HISTORY){
            HistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}