package thesis.project.omrscanner.main

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.opencv.android.OpenCVLoader
import thesis.project.omrscanner.ui.theme.ThemeAww

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable Firestore offline persistence
        val db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings
        Log.i("Firestore", "Offline persistence enabled")

        // Initialize OpenCV
        @Suppress("DEPRECATION")
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Initialization failed")
        } else {
            Log.i("OpenCV", "OpenCV initialized successfully")
        }

        // One-time admin creation (run only if no admins exist)
        createDefaultAdminIfNone()

        // Set Compose UI
        setContent {
            ThemeAww {
                val navController = rememberNavController()
                AppNavigation(navController)
            }
        }
    }

    private fun createDefaultAdminIfNone() {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        // Run in a coroutine scope
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if any admin already exists
                val adminQuery = firestore.collection("users")
                    .whereEqualTo("isAdmin", true)
                    .get()
                    .await()

                if (adminQuery.isEmpty) {
                    // No admin exists, create default admin
                    val email = "omrscannerapp@gmail.com"
                    val password = "admin123"

                    // Check if the user already exists in Authentication (avoid duplication)
                    val userExists = auth.fetchSignInMethodsForEmail(email)
                        .await().signInMethods?.isNotEmpty() == true
                    val uid = if (!userExists) {
                        val result = auth.createUserWithEmailAndPassword(email, password).await()
                        result.user?.uid ?: throw Exception("Failed to get UID for default admin")
                    } else {
                        // Get UID from Firestore if already exists in Authentication
                        val doc = firestore.collection("users")
                            .whereEqualTo("email", email)
                            .get()
                            .await()
                        doc.documents.firstOrNull()?.id
                            ?: throw Exception("Default admin exists in Auth but not in Firestore")
                    }

                    // Add or update Firestore document
                    firestore.collection("users").document(uid).set(
                        mapOf(
                            "email" to email,
                            "isAdmin" to true,
                            "approved" to true
                        )
                    ).await()

                    Log.i("AdminSetup", "Default admin ensured: $email / $password")
                } else {
                    Log.i("AdminSetup", "Admin already exists, skipping creation")
                }
            } catch (e: Exception) {
                Log.e("AdminSetup", "Failed to ensure default admin: ${e.localizedMessage}")
            }
        }
    }
}