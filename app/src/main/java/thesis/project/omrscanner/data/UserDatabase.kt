package thesis.project.omrscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.Callback
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import thesis.project.omrscanner.auth.AppUser
import thesis.project.omrscanner.auth.UserDao

@Database(
    entities = [AppUser::class],
    version = 2,  // incremented to prevent Room crash
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
                    .fallbackToDestructiveMigration() // reset DB if version changes
                    .addCallback(UserDatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class UserDatabaseCallback(
        private val context: Context
    ) : Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Pre-insert admin user
            CoroutineScope(Dispatchers.IO).launch {
                val adminUser = AppUser(
                    email = "admin@omrscanner.com",
                    password = "admin123",  // change to secure password
                    name = "Administrator",
                    role = "admin"
                )
                getInstance(context).userDao().insert(adminUser)
            }
        }
    }
}
