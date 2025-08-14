package thesis.project.omrscanner.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import thesis.project.omrscanner.data.UserDatabase

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = UserDatabase.getInstance(application).userDao()

    // Login by email
    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val user = userDao.getUserByEmail(email)
            if (user != null) {
                if (user.role != "pending") { // check if approved
                    if (user.password == password) {
                        onResult(true, user.role)
                    } else {
                        onResult(false, "Invalid email or password")
                    }
                } else {
                    onResult(false, "Account not approved yet")
                }
            } else {
                onResult(false, "Invalid email or password")
            }
        }
    }

    // Submit new signup request
    fun submitSignupRequest(password: String, email: String, name: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val newUser = AppUser(
                email = email,
                password = password,
                name = name,
                role = "pending" // pending approval
            )
            userDao.insert(newUser)
            onResult(true, "Signup request submitted successfully")
        }
    }

    // Get pending requests (role = "pending")
    fun getPendingRequests(onResult: (List<AppUser>) -> Unit) {
        viewModelScope.launch {
            onResult(userDao.getPendingRequests())
        }
    }

    // Approve user (change role to "user")
    fun approveRequest(user: AppUser, onResult: () -> Unit) {
        viewModelScope.launch {
            userDao.update(user.copy(role = "user"))
            onResult()
        }
    }

    // Delete user
    fun deleteUser(user: AppUser, onResult: () -> Unit) {
        viewModelScope.launch {
            userDao.delete(user)
            onResult()
        }
    }

    // Get all approved users (role != "pending")
    fun getApprovedUsers(onResult: (List<AppUser>) -> Unit) {
        viewModelScope.launch {
            onResult(userDao.getApprovedUsers())
        }
    }
}
