package thesis.project.omrscanner.auth

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_user")
data class AppUser(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val password: String,   // hashed or plain (for now)
    val email: String,
    val name: String,
    val role: String = "user", // "admin" or "user"
    val isApproved: Boolean = false
)