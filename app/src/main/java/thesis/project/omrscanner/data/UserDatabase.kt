package thesis.project.omrscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import thesis.project.omrscanner.auth.AppUser
import thesis.project.omrscanner.auth.UserDao

@Database(
    entities = [AppUser::class],
    version = 2, // increment version if schema changed
    exportSchema = false
)
abstract class UserDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: UserDatabase? = null

        fun getInstance(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "user_database"
                )
                    .fallbackToDestructiveMigration() // reset DB on version change
                    .addCallback(UserDatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class UserDatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val userDao = getInstance(context).userDao()

                // Ensure admin exists and is approved
                val adminEmail = "omrscannerapp@gmail.com"
                val existingAdmin = userDao.getUserByEmail(adminEmail)
                if (existingAdmin == null) {
                    // Insert new admin
                    val adminUser = AppUser(
                        email = adminEmail,
                        password = "admin123",
                        name = "Administrator",
                        role = "admin",
                        isApproved = true
                    )
                    userDao.insert(adminUser)
                } else if (!existingAdmin.isApproved) {
                    // Update existing admin to be approved
                    userDao.update(existingAdmin.copy(isApproved = true))
                }
            }
        }
    }
}
