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

    // Start destination is initially null until we check preferences
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val email = prefs.getString("email", "") ?: ""
        val isAdmin = prefs.getBoolean("isAdmin", false)

        startDestination = when {
            isAdmin -> "admin"
            email.isNotBlank() -> "user_login" // User login if email exists
            else -> "user_login" // Default to user login
        }
    }

    // Only render NavHost when startDestination is ready
    startDestination?.let { start ->
        NavHost(navController = navController, startDestination = start) {

            composable("user_login") { UserLoginScreen(navController) }
            composable("signup_request") { SignupRequestScreen(navController) }
            composable("admin_login") { AdminLoginScreen(navController) }
            composable("admin") { AdminScreen(navController) }
            composable("menu") { MainMenu(navController) }
            composable("create") { CreateScreen(navController) }
            composable("scan") { ScanScreen(navigateToResult = { navController.navigate("result") }) }
            composable("result") { ResultScreen() }
        }
    }
}
