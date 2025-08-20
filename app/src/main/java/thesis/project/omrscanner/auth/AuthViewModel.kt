package thesis.project.omrscanner.auth

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext

    /** User login */
    suspend fun login(email: String, password: String, context: Context): Pair<Boolean, String?> {
        val result = UserManager.login(email, password)
        return if (result.isSuccess) {
            val data = result.getOrNull() ?: emptyMap()
            val approved = data["approved"] as? Boolean ?: false
            val isAdmin = data["isAdmin"] as? Boolean ?: false

            if (!approved) {
                Pair(false, "Account not approved yet.")
            } else {
                // Save login state
                AuthDataStore.saveUserLogin(context, email, isAdmin)
                if (isAdmin) adminLoggedIn(context)
                Pair(true, if (isAdmin) "admin" else "user")
            }
        } else {
            Pair(false, result.exceptionOrNull()?.localizedMessage ?: "Login failed")
        }
    }

    /** User logout */
    fun logout(context: Context) {
        UserManager.logout()
        viewModelScope.launch {
            AuthDataStore.clearUserLogin(context)
        }
    }

    /** Check if admin is logged in */
    suspend fun isAdminLoggedIn(context: Context): Boolean {
        return AuthDataStore.isAdminLoggedIn(context).first()
    }

    /** Admin login state */
    fun adminLoggedIn(context: Context) {
        viewModelScope.launch { AuthDataStore.saveAdminLogin(context, true) }
    }

    fun adminLoggedOut(context: Context) {
        viewModelScope.launch { AuthDataStore.saveAdminLogin(context, false) }
    }

    /** Submit signup request for regular users */
    fun submitSignupRequest(
        email: String,
        password: String,
        isAdmin: Boolean = false
    ): Pair<Boolean, String> {
        return try {
            viewModelScope.launch {
                val result = UserManager.signup(email, password, isAdmin)
                if (result.isFailure) throw result.exceptionOrNull() ?: Exception("Signup failed")
            }
            Pair(true, "Signup request submitted successfully")
        } catch (e: Exception) {
            Pair(false, "Failed to submit signup request: ${e.localizedMessage}")
        }
    }

    /** Admin-only: get pending signup requests */
    suspend fun getPendingRequests(): List<Map<String, Any>> {
        return UserManager.getPendingRequests()
    }

    /** Admin-only: approve a user by UID */
    suspend fun approveRequest(uid: String): Pair<Boolean, String> {
        return try {
            UserManager.approveUser(uid)
            Pair(true, "User approved successfully")
        } catch (e: Exception) {
            Pair(false, "Failed to approve user: ${e.localizedMessage}")
        }
    }

    /** Admin-only: delete a user by UID */
    suspend fun deleteUser(uid: String): Pair<Boolean, String> {
        return try {
            UserManager.deleteUser(uid)
            Pair(true, "User deleted successfully")
        } catch (e: Exception) {
            Pair(false, "Failed to delete user: ${e.localizedMessage}")
        }
    }
}
