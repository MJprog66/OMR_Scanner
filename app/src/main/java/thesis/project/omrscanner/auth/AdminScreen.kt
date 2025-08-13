package thesis.project.omrscanner.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var signupRequests by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Listen to pending signup requests
    LaunchedEffect(Unit) {
        db.collection("signup_requests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                signupRequests = snapshot?.documents?.map { it.data!! } ?: emptyList()
            }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pending Signup Requests") }) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (signupRequests.isEmpty()) {
                Text(
                    "No pending requests",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(signupRequests) { request ->
                        val email = request["email"] as? String ?: ""
                        val name = request["name"] as? String ?: ""

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
                                Text("Name: $name", style = MaterialTheme.typography.bodyLarge)
                                Text("Email: $email", style = MaterialTheme.typography.bodyMedium)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Button(
                                        onClick = { approveRequest(db, email, name, context) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Approve", color = MaterialTheme.colorScheme.onPrimary)
                                    }

                                    Button(
                                        onClick = { denyRequest(db, email, context) },
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
}

private fun approveRequest(
    db: FirebaseFirestore,
    email: String,
    name: String,
    context: android.content.Context
) {
    val requestRef = db.collection("signup_requests").document(email)
    val allowedRef = db.collection("allowed_users").document(email)

    val userData = hashMapOf(
        "email" to email,
        "name" to name,
        "role" to "user",
        "isAdmin" to false
    )

    allowedRef.set(userData)
        .addOnSuccessListener {
            requestRef.delete()
            Toast.makeText(context, "$email approved and added to allowed users!", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to approve $email", Toast.LENGTH_SHORT).show()
        }
}

private fun denyRequest(db: FirebaseFirestore, email: String, context: android.content.Context) {
    db.collection("signup_requests").document(email)
        .delete()
        .addOnSuccessListener {
            Toast.makeText(context, "$email denied!", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to deny $email", Toast.LENGTH_SHORT).show()
        }
}
