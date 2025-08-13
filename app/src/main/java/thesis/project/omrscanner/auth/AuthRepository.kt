package thesis.project.omrscanner.auth

import android.content.Context
import android.widget.Toast
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

object AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Login a user or admin with role verification.
     *
     * @param email User email
     * @param password User password
     * @param context Context for Toast
     * @param navController NavController for navigation
     * @param onComplete Callback for login success/failure
     * @param isAdminLogin True if admin login, false if regular user
     */
    fun loginUser(
        email: String,
        password: String,
        context: Context,
        navController: NavController,
        onComplete: (Boolean) -> Unit,
        isAdminLogin: Boolean = false
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    onComplete(false)
                    Toast.makeText(context, "Login failed: UID not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                firestore.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        val role = doc.getString("role")
                        val approved = doc.getBoolean("approved") ?: false

                        when {
                            !approved -> {
                                auth.signOut()
                                onComplete(false)
                                Toast.makeText(context, "Account pending approval", Toast.LENGTH_SHORT).show()
                            }
                            isAdminLogin && role == "admin" -> {
                                onComplete(true)
                                navController.navigate("admin_home") {
                                    popUpTo("admin_login") { inclusive = true }
                                }
                            }
                            !isAdminLogin && role == "user" -> {
                                onComplete(true)
                                navController.navigate("user_home") {
                                    popUpTo("user_login") { inclusive = true }
                                }
                            }
                            else -> {
                                auth.signOut()
                                onComplete(false)
                                val msg = if (isAdminLogin) "Not authorized as admin" else "Not authorized as user"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        auth.signOut()
                        onComplete(false)
                        Toast.makeText(context, "Failed to fetch user data: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                onComplete(false)
                Toast.makeText(context, "Login failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Submit a signup request for admin approval.
     * Email is used as the document ID to prevent duplicate requests.
     */
    fun submitSignupRequest(
        email: String,
        name: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val request = hashMapOf(
            "email" to email,
            "name" to name,
            "approved" to false,
            "requestedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("signup_requests")
            .document(email)
            .set(request)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { e -> onComplete(false, e.localizedMessage) }
    }
}
