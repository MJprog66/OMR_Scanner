package thesis.project.omrscanner.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class SignupRequest(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val approved: Boolean = false,
    val requestedAt: Long = 0L
)

@Composable
fun AdminScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var signupRequests by remember { mutableStateOf(listOf<SignupRequest>()) }
    var loading by remember { mutableStateOf(true) }

    // Listener reference to remove it if needed
    var listenerRegistration: ListenerRegistration? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        listenerRegistration = firestore.collection("signup_requests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    loading = false
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.map { doc ->
                    val uid = doc.getString("uid") ?: ""
                    val name = doc.getString("name") ?: ""
                    val email = doc.getString("email") ?: ""
                    val approved = doc.getBoolean("approved") ?: false
                    val timestamp: Timestamp? = doc.getTimestamp("requestedAt")
                    val requestedAtMillis = timestamp?.toDate()?.time ?: 0L

                    SignupRequest(
                        uid = uid,
                        name = name,
                        email = email,
                        approved = approved,
                        requestedAt = requestedAtMillis
                    )
                } ?: emptyList()

                signupRequests = requests.sortedByDescending { it.requestedAt }
                loading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Signup Requests",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else {
            if (signupRequests.isEmpty()) {
                Text("No signup requests found.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(signupRequests) { request ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Name: ${request.name}")
                                Text("Email: ${request.email}")
                                Text("Approved: ${request.approved}")
                                Text("Requested At: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                    .format(java.util.Date(request.requestedAt))}")
                            }
                        }
                    }
                }
            }
        }
    }

    // Remove listener when composable leaves composition
    DisposableEffect(Unit) {
        onDispose { listenerRegistration?.remove() }
    }
}
