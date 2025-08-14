package thesis.project.omrscanner.auth

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import thesis.project.omrscanner.data.UserDatabase

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = UserDatabase.getInstance(application).userDao()

    /** User login - suspend function */
    suspend fun login(email: String, password: String, context: Context): Pair<Boolean, String?> {
        val user = userDao.login(email, password)
        return if (user != null) {
            if (user.isApproved) {
                // Save login state
                AuthDataStore.saveUserLogin(context, user.email, user.role == "admin")
                Pair(true, user.role)
            } else {
                Pair(false, "Account not approved yet.")
            }
        } else {
            Pair(false, "Invalid email or password")
        }
    }

    /** Logout user */
    fun logout(context: Context) {
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

    /** Submit signup request */
    fun submitSignupRequest(
        password: String,
        email: String,
        name: String
    ): Pair<Boolean, String> {
        return try {
            val newUser = AppUser(
                email = email,
                password = password,
                name = name,
                role = "user",
                isApproved = false
            )
            viewModelScope.launch {
                userDao.insert(newUser)
            }
            Pair(true, "Signup request submitted successfully")
        } catch (e: Exception) {
            Pair(false, "Failed to submit signup request: ${e.message}")
        }
    }

    /** Get pending requests */
    suspend fun getPendingRequests(): List<AppUser> {
        return userDao.getPendingRequests()
    }

    /** Get approved users */
    suspend fun getApprovedUsers(): List<AppUser> {
        return userDao.getApprovedUsers()
    }

    /** Admin approve or delete user */
    suspend fun approveRequest(user: AppUser) {
        userDao.update(user.copy(isApproved = true))
    }

    suspend fun deleteUser(user: AppUser) {
        userDao.delete(user)
    }
}
