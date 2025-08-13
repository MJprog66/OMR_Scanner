package thesis.project.omrscanner.auth

import android.widget.Toast
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var signupRequests by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    val context = LocalContext.current

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
        if (signupRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No pending requests")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
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
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Name: $name", style = MaterialTheme.typography.bodyLarge)
                            Text("Email: $email", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = {
                                        approveRequest(db, email, context) {
                                            signupRequests = signupRequests.filter { it["email"] != email }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Approve", color = MaterialTheme.colorScheme.onPrimary)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Button(
                                    onClick = {
                                        denyRequest(db, email, context) {
                                            signupRequests = signupRequests.filter { it["email"] != email }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f)
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

private fun approveRequest(
    db: FirebaseFirestore,
    email: String,
    context: android.content.Context,
    onSuccess: () -> Unit
) {
    val docRef = db.collection("signup_requests").document(email)
    docRef.update("approved", true)
        .addOnSuccessListener {
            Toast.makeText(context, "$email approved!", Toast.LENGTH_SHORT).show()
            onSuccess()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to approve $email: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

private fun denyRequest(
    db: FirebaseFirestore,
    email: String,
    context: android.content.Context,
    onSuccess: () -> Unit
) {
    val docRef = db.collection("signup_requests").document(email)
    docRef.delete()
        .addOnSuccessListener {
            Toast.makeText(context, "$email denied!", Toast.LENGTH_SHORT).show()
            onSuccess()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to deny $email: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}
