package thesis.project.omrscanner.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToUserLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Check persistent login
    LaunchedEffect(Unit) {
        val sharedPref = context.getSharedPreferences("login_prefs", 0)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        val userType = sharedPref.getString("userType", "")
        if (isLoggedIn && userType == "admin") {
            onLoginSuccess()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Admin Login", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Enter email and password") }
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        try {
                            val auth = FirebaseAuth.getInstance()
                            val firestore = FirebaseFirestore.getInstance()

                            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
                            val uid = result.user?.uid

                            if (uid == null) {
                                snackbarHostState.showSnackbar("Login failed: no UID")
                                isLoading = false
                                return@launch
                            }

                            val doc = firestore.collection("users").document(uid).get().await()
                            val isAdmin = doc.getBoolean("isAdmin") ?: false
                            val approved = doc.getBoolean("approved") ?: false

                            if (isAdmin && approved) {
                                // Save persistent login
                                val sharedPref = context.getSharedPreferences("login_prefs", 0)
                                with(sharedPref.edit()) {
                                    putBoolean("isLoggedIn", true)
                                    putString("userType", "admin")
                                    putString("userEmail", email.trim())
                                    apply()
                                }

                                snackbarHostState.showSnackbar("Admin login successful!")
                                onLoginSuccess()
                            } else {
                                auth.signOut()
                                snackbarHostState.showSnackbar("You are not an approved admin")
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Login failed: ${e.localizedMessage}")
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Login as Admin")
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onNavigateToSignup) { Text("Signup Request") }
                TextButton(onClick = onNavigateToUserLogin) { Text("User Login") }
            }
        }
    }
}
