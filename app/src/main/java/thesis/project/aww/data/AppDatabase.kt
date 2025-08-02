package thesis.project.aww.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import thesis.project.aww.result.StudentResult
import thesis.project.aww.result.StudentResultDAO

@Database(entities = [StudentResult::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentResultDao(): StudentResultDAO

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "omr_results_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
