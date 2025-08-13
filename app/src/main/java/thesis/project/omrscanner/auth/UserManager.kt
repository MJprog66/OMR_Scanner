package thesis.project.omrscanner.auth

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object UserManager {

    /**
     * Checks approval status with offline-first approach.
     */
    suspend fun isUserApproved(email: String, context: Context): Boolean {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        // 1. Offline check first
        val locallyApproved = prefs.getBoolean("approved", false)
        if (locallyApproved) {
            // Trigger background refresh without blocking
            refreshApprovalStatus(email, context)
            return true
        }

        // 2. If not approved locally, check online
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("signup_requests")
                .document(email)
                .get()
                .await()

            val approvedOnline = doc.getBoolean("approved") ?: false
            if (approvedOnline) {
                saveApprovalLocally(email, context)
            }
            approvedOnline
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Refresh approval status in the background (non-blocking).
     */
    fun refreshApprovalStatus(email: String, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("signup_requests")
                    .document(email)
                    .get()
                    .await()

                val approvedOnline = doc.getBoolean("approved") ?: false
                if (approvedOnline) {
                    saveApprovalLocally(email, context)
                }
            } catch (_: Exception) {
                // Ignore errors — keep local status
            }
        }
    }

    /**
     * Save approval status locally for offline access.
     */
    private fun saveApprovalLocally(email: String, context: Context) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("approved", true)
            .putString("email", email)
            .apply()
    }
}
