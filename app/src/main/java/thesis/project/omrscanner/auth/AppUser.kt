package thesis.project.omrscanner.auth

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class AppUser(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val password: String,
    val name: String,
    val role: String = "user",     // "user" or "admin"
    val isApproved: Boolean = false
)
