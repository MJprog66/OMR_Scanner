package thesis.project.omrscanner.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingUsers by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var approvedUsers by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Pending, 1 = Approved

    // Function to refresh lists
    suspend fun refreshLists() {
        pendingUsers = UserManager.getPendingRequests()
        approvedUsers = UserManager.getAllApprovedUsers()
    }

    LaunchedEffect(Unit) { scope.launch { refreshLists() } }

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
                    containerColor = Color(0xFF0D47A1), // dark blue
                    titleContentColor = Color.White
                ),
                actions = {
                    TextButton(onClick = {
                        UserManager.logout()
                        onLogout()
                    }) {
                        Text("Logout", color = Color.White) // always visible
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

            // Tabs with counts
            val tabs = listOf(
                "Pending Users (${pendingUsers.size})",
                "Approved Users (${approvedUsers.size})"
            )

            ScrollableTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }) {
                        Text(title, modifier = Modifier.padding(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            when (selectedTab) {
                0 -> UserList(
                    users = filteredPending,
                    isPending = true,
                    snackbarHostState = snackbarHostState,
                    onActionComplete = { scope.launch { refreshLists() } }
                )
                1 -> UserList(
                    users = filteredApproved,
                    isPending = false,
                    snackbarHostState = snackbarHostState,
                    onActionComplete = { scope.launch { refreshLists() } }
                )
            }
        }
    }
}

@Composable
fun UserList(
    users: List<Map<String, Any>>,
    isPending: Boolean,
    snackbarHostState: SnackbarHostState,
    onActionComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()

    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = if (isPending) "No pending requests" else "No approved users",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(users) { user ->
                val uid = user["uid"] as? String ?: return@items
                val email = user["email"] as? String ?: "Unknown"
                val role = if (user["isAdmin"] as? Boolean == true) "Admin" else "User"

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(email, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Role: $role",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier
                                    .background(
                                        if (role == "Admin") Color(0xFF4CAF50) else Color(0xFF2196F3)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Row {
                            if (isPending) {
                                Button(onClick = {
                                    scope.launch {
                                        UserManager.approveUser(uid)
                                        snackbarHostState.showSnackbar("$email approved")
                                        onActionComplete()
                                    }
                                }) { Text("Approve") }

                                Spacer(Modifier.width(8.dp))
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        UserManager.deleteUser(uid)
                                        snackbarHostState.showSnackbar("$email deleted")
                                        onActionComplete()
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFD32F2F))
                            ) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}
