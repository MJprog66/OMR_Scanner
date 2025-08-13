package thesis.project.omrscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import thesis.project.omrscanner.result.StudentResult
import thesis.project.omrscanner.result.StudentResultDAO
import thesis.project.omrscanner.create.OmrSheet
import thesis.project.omrscanner.result.Section
import thesis.project.omrscanner.result.SectionDAO

@Database(
    entities = [StudentResult::class, OmrSheet::class, Section::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studentResultDao(): StudentResultDAO
    abstract fun sectionDao(): SectionDAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from database version 1 to 2:
         * Adds the 'sections' table with id, sheetTitle, and name columns.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sections` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sheetTitle` TEXT NOT NULL,
                        `name` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "omr_results_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
