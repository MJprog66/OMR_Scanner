package thesis.project.omrscanner.auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingUsers by remember { mutableStateOf(listOf<AppUser>()) }
    var approvedUsers by remember { mutableStateOf(listOf<AppUser>()) }
    var searchQuery by remember { mutableStateOf("") }

    // Load lists on start
    LaunchedEffect(Unit) {
        pendingUsers = authViewModel.getPendingRequests()
        approvedUsers = authViewModel.getApprovedUsers()
    }

    // Filter lists based on search
    val filteredPending = pendingUsers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true)
    }
    val filteredApproved = approvedUsers
        .filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.email.contains(searchQuery, ignoreCase = true)
        }
        .sortedBy { if (it.role.lowercase() == "admin") 0 else 1 }

    // Merge into one list with headers
    val combinedList = buildList<Pair<String, AppUser?>> {
        if (filteredPending.isNotEmpty()) {
            add("Pending Requests" to null)
            filteredPending.forEach { add("" to it) }
        }
        if (filteredApproved.isNotEmpty()) {
            add("Approved Users" to null)
            filteredApproved.forEach { add("" to it) }
        }
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
                        scope.launch {
                            authViewModel.adminLoggedOut(context)
                            onLogout()
                        }
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
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name or email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(combinedList) { (header, user) ->
                    if (user == null) {
                        // Section header
                        Text(
                            text = header,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
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
                                    Text(user.name, style = MaterialTheme.typography.titleMedium)
                                    Text(user.email, style = MaterialTheme.typography.bodyMedium)
                                    if (user.isApproved) {
                                        val roleColor =
                                            if (user.role.lowercase() == "admin") Color(0xFF4CAF50) else Color(0xFF2196F3)
                                        Text(
                                            text = "Role: ${user.role}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .background(roleColor)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row {
                                    if (!user.isApproved) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    authViewModel.approveRequest(user)
                                                    pendingUsers = pendingUsers.filter { it.email != user.email }
                                                    approvedUsers = approvedUsers + user.copy(isApproved = true)
                                                    snackbarHostState.showSnackbar("${user.name} approved")
                                                }
                                            },
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) { Text("Approve") }
                                    }

                                    OutlinedButton(
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White,
                                            containerColor = Color(0xFFD32F2F)
                                        ),
                                        onClick = {
                                            scope.launch {
                                                authViewModel.deleteUser(user)
                                                pendingUsers = pendingUsers.filter { it.email != user.email }
                                                approvedUsers = approvedUsers.filter { it.email != user.email }
                                                snackbarHostState.showSnackbar("${user.name} deleted")
                                            }
                                        }
                                    ) { Text("Delete") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
