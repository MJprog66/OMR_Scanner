package thesis.project.aww.main

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import thesis.project.aww.create.CreateScreen
import thesis.project.aww.result.ResultScreen
import thesis.project.aww.scan.ScanScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "menu"
    ) {
        composable("menu") {
            MainMenu(navController)
        }
        composable("create") {
            CreateScreen(navController)
        }
        composable("scan") {
            ScanScreen(
                navigateToResult = { navController.navigate("result") }
            )
        }
        composable("result") {
            ResultScreen()
        }
    }
}
