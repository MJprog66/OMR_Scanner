package thesis.project.omrscanner.auth

import androidx.room.*

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: AppUser)

    @Query("SELECT * FROM app_user WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): AppUser?

    @Query("SELECT * FROM app_user WHERE role != 'pending'")
    suspend fun getApprovedUsers(): List<AppUser>

    @Query("SELECT * FROM app_user WHERE role = 'pending'")
    suspend fun getPendingRequests(): List<AppUser>

    @Update
    suspend fun update(user: AppUser)

    @Delete
    suspend fun delete(user: AppUser)
}
