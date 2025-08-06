package thesis.project.aww.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import thesis.project.aww.result.StudentResult
import thesis.project.aww.result.StudentResultDAO
import thesis.project.aww.create.OmrSheet

@Database(entities = [StudentResult::class, OmrSheet::class], version = 2, exportSchema = false
)
@TypeConverters(Converters::class)
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
