package thesis.project.omrscanner.main

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import thesis.project.omrscanner.auth.*
import thesis.project.omrscanner.create.CreateScreen
import thesis.project.omrscanner.result.ResultScreen
import thesis.project.omrscanner.scan.ScanScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val email = prefs.getString("email", "") ?: ""
        val isAdmin = prefs.getBoolean("isAdmin", false)

        startDestination = when {
            isAdmin -> "admin"
            email.isNotBlank() -> "user_login"
            else -> "user_login"
        }
    }

    startDestination?.let { start ->
        NavHost(navController = navController, startDestination = start) {

            // User login
            composable("user_login") {
                UserLoginScreen(
                    onLoginSuccess = { navController.navigate("menu") },
                    onNavigateToSignup = { navController.navigate("signup_request") },
                    onNavigateToAdminLogin = { navController.navigate("admin_login") }
                )
            }

            // Signup request
            composable("signup_request") {
                SignupRequestScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Admin login
            composable("admin_login") {
                AdminLoginScreen(
                    onLoginSuccess = { navController.navigate("admin") },
                    onNavigateToSignup = { navController.navigate("signup_request") },
                    onNavigateToUserLogin = { navController.navigate("user_login") }
                )
            }

            // Admin pending requests screen
            composable("admin") {
                AdminScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Existing screens remain unchanged
            composable("menu") { MainMenu(navController) }
            composable("create") { CreateScreen(navController) }
            composable("scan") { ScanScreen(navigateToResult = { navController.navigate("result") }) }
            composable("result") { ResultScreen() }
        }
    }
}
