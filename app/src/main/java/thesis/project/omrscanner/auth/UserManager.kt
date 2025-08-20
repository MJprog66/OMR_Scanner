package thesis.project.omrscanner.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object UserManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Signup function (existing)
    suspend fun signup(email: String, password: String, isAdmin: Boolean = false) = try {
        auth.createUserWithEmailAndPassword(email, password).await()
        val uid = auth.currentUser?.uid ?: throw Exception("Signup failed")
        firestore.collection("users").document(uid)
            .set(
                mapOf(
                    "email" to email,
                    "isAdmin" to isAdmin,
                    "approved" to !isAdmin // Admin auto-approved
                )
            ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Login function (existing)
    suspend fun login(email: String, password: String) = try {
        auth.signInWithEmailAndPassword(email, password).await()
        val uid = auth.currentUser?.uid ?: throw Exception("Login failed")
        val doc = firestore.collection("users").document(uid).get().await()
        Result.success(doc.data ?: emptyMap<String, Any>())
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Logout
    fun logout() {
        auth.signOut()
    }

    // Admin-only: get pending requests
    suspend fun getPendingRequests(): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("approved", false)
                .get().await()
            snapshot.documents.map { it.data!!.plus("uid" to it.id) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Admin-only: get all approved users
    suspend fun getAllApprovedUsers(): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("approved", true)
                .get().await()
            snapshot.documents.map { it.data!!.plus("uid" to it.id) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Admin-only: approve user
    suspend fun approveUser(uid: String) {
        firestore.collection("users").document(uid)
            .update("approved", true).await()
    }

    // Admin-only: delete user
    suspend fun deleteUser(uid: String) {
        firestore.collection("users").document(uid).delete().await()
    }
}
