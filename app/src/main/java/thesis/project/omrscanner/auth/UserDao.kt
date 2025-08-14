package thesis.project.omrscanner.auth

import androidx.room.*

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: AppUser)

    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    suspend fun login(email: String, password: String): AppUser?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): AppUser?

    @Query("SELECT * FROM users WHERE isApproved = 0")
    suspend fun getPendingRequests(): List<AppUser>

    @Query("SELECT * FROM users WHERE isApproved = 1")
    suspend fun getApprovedUsers(): List<AppUser>

    @Update
    suspend fun update(user: AppUser)

    @Delete
    suspend fun delete(user: AppUser)
}
