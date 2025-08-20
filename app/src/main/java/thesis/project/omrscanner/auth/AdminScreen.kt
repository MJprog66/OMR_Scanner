package thesis.project.omrscanner.auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingUsers by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var approvedUsers by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var searchQuery by remember { mutableStateOf("") }

    // Function to refresh lists
    suspend fun refreshLists() {
        pendingUsers = UserManager.getPendingRequests()
        approvedUsers = UserManager.getAllApprovedUsers()
    }

    // Load lists on start
    LaunchedEffect(Unit) {
        refreshLists()
    }

    val filteredPending = pendingUsers.filter {
        (it["email"] as? String)?.contains(searchQuery, ignoreCase = true) == true
    }

    val filteredApproved = approvedUsers.filter {
        (it["email"] as? String)?.contains(searchQuery, ignoreCase = true) == true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D47A1),
                    titleContentColor = Color.White
                ),
                actions = {
                    TextButton(onClick = {
                        UserManager.logout()
                        onLogout()
                    }) {
                        Text("Logout", color = Color.White)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (filteredPending.isNotEmpty()) {
                    item { Text("Pending Users", style = MaterialTheme.typography.headlineSmall) }
                    items(filteredPending) { user ->
                        UserCard(user, isPending = true, snackbarHostState) {
                            // Refresh after approve/delete
                            scope.launch { refreshLists() }
                        }
                    }
                }

                if (filteredApproved.isNotEmpty()) {
                    item { Text("Approved Users", style = MaterialTheme.typography.headlineSmall) }
                    items(filteredApproved) { user ->
                        UserCard(user, isPending = false, snackbarHostState) {
                            // Refresh after delete
                            scope.launch { refreshLists() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserCard(
    user: Map<String, Any>,
    isPending: Boolean,
    snackbarHostState: SnackbarHostState,
    onActionComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(user["email"] as? String ?: "", style = MaterialTheme.typography.titleMedium)
                val role = if (user["isAdmin"] as? Boolean == true) "Admin" else "User"
                Text(
                    text = "Role: $role",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(if (role == "Admin") Color(0xFF4CAF50) else Color(0xFF2196F3))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Row {
                if (isPending) {
                    Button(onClick = {
                        scope.launch {
                            val uid = user["uid"] as? String ?: return@launch
                            UserManager.approveUser(uid)
                            snackbarHostState.showSnackbar("${user["email"]} approved")
                            onActionComplete()
                        }
                    }) { Text("Approve") }
                }

                Spacer(Modifier.width(8.dp))

                OutlinedButton(
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        containerColor = Color(0xFFD32F2F)
                    ),
                    onClick = {
                        scope.launch {
                            val uid = user["uid"] as? String ?: return@launch
                            UserManager.deleteUser(uid)
                            snackbarHostState.showSnackbar("${user["email"]} deleted")
                            onActionComplete()
                        }
                    }
                ) { Text("Delete") }
            }
        }
    }
}
