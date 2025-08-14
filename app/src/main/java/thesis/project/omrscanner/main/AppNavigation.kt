package thesis.project.omrscanner.main

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch
import thesis.project.omrscanner.auth.*
import thesis.project.omrscanner.create.CreateScreen
import thesis.project.omrscanner.scan.ScanScreen
import thesis.project.omrscanner.result.ResultScreen

@Composable
fun AppNavigation(navController: NavHostController, authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Holds the start destination
    var startDestination by remember { mutableStateOf<String?>(null) }

    // Load admin login state from DataStore
    LaunchedEffect(Unit) {
        val isAdmin = authViewModel.isAdminLoggedIn(context)
        startDestination = if (isAdmin) "admin" else "user_login"
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

            // Signup request screen
            composable("signup_request") {
                SignupRequestScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToAdminLogin = { navController.navigate("admin_login") },
                    onNavigateToUserLogin = { navController.navigate("user_login") }
                )
            }

            // Admin login
            composable("admin_login") {
                AdminLoginScreen(
                    onLoginSuccess = {
                        // Persist admin login in DataStore
                        scope.launch {
                            authViewModel.adminLoggedIn(context)
                        }
                        navController.navigate("admin") {
                            popUpTo("admin_login") { inclusive = true }
                        }
                    },
                    onNavigateToSignup = { navController.navigate("signup_request") },
                    onNavigateToUserLogin = { navController.navigate("user_login") }
                )
            }

            // Admin dashboard
            composable("admin") {
                AdminScreen(
                    onLogout = {
                        // Clear DataStore login
                        scope.launch {
                            authViewModel.adminLoggedOut(context)
                            navController.navigate("admin_login") {
                                popUpTo("admin") { inclusive = true }
                            }
                        }
                    }
                )
            }

            // Main menu
            composable("menu") { MainMenu(navController) }

            // Create screen
            composable("create") { CreateScreen(navController) }

            // Scan screen
            composable("scan") { ScanScreen(navigateToResult = { navController.navigate("result") }) }

            // Result screen
            composable("result") { ResultScreen() }
        }
    }
}
