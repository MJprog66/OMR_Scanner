package thesis.project.omrscanner.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit
) {
    val authViewModel: AuthViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var requests by remember { mutableStateOf(listOf<AppUser>()) }
    var showDialog by remember { mutableStateOf(false) }
    var approvedUser by remember { mutableStateOf<AppUser?>(null) }

    // Load pending signup requests
    LaunchedEffect(Unit) {
        authViewModel.getPendingRequests { list ->
            requests = list
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Signup Requests") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (requests.isEmpty()) {
                Text("No pending requests", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(requests) { user ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Name: ${user.name}", style = MaterialTheme.typography.bodyLarge)
                                Text("Email: ${user.email}", style = MaterialTheme.typography.bodyMedium)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Approve button
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                authViewModel.approveRequest(user) {
                                                    approvedUser = user
                                                    showDialog = true
                                                    // Refresh the list
                                                    authViewModel.getPendingRequests { updated ->
                                                        requests = updated
                                                    }
                                                }
                                                snackbarHostState.showSnackbar("User ${user.email} approved")
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Approve", color = MaterialTheme.colorScheme.onPrimary)
                                    }

                                    // Deny button
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                authViewModel.deleteUser(user) {
                                                    authViewModel.getPendingRequests { updated ->
                                                        requests = updated
                                                    }
                                                }
                                                snackbarHostState.showSnackbar("Request denied for ${user.email}")
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Deny", color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Approval dialog
    if (showDialog && approvedUser != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("User Approved") },
            text = { Text("User ${approvedUser!!.email} has been approved!") },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
