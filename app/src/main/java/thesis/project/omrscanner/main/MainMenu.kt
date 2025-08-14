package thesis.project.omrscanner.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import thesis.project.omrscanner.auth.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import androidx.compose.ui.platform.LocalContext

@Composable
fun MainMenu(navController: NavHostController) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Main Menu", fontSize = 28.sp)
            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { navController.navigate("create") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("scan") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("result") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Result")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Logout button
            Button(
                onClick = {
                    scope.launch {
                        // Clear user/admin login state
                        authViewModel.logout(context)
                        authViewModel.adminLoggedOut(context)
                        // Navigate to login screen (user_login route)
                        navController.navigate("user_login") {
                            popUpTo("mainMenu") { inclusive = true } // Remove MainMenu from backstack
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout", color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}
