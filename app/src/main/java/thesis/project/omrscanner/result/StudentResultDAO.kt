package thesis.project.omrscanner.result

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete

@Dao
interface StudentResultDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: StudentResult)

    @Query("SELECT * FROM student_results WHERE sheetTitle = :title ORDER BY id DESC")
    suspend fun getResultsByTitle(title: String): List<StudentResult>

    @Query("SELECT * FROM student_results WHERE sheetTitle = :sheetTitle AND section = :sectionName ORDER BY id DESC")
    suspend fun getResultsByTitleAndSection(sheetTitle: String, sectionName: String): List<StudentResult>

    @Delete
    suspend fun deleteResult(result: StudentResult): Int

    @Query("DELETE FROM student_results WHERE sheetTitle = :title")
    suspend fun deleteResultsByTitle(title: String)

    @Query("SELECT DISTINCT sheetTitle FROM student_results")
    suspend fun getAllSheetTitles(): List<String>

    // Update section name in student_results for a given sheetTitle
    @Query("UPDATE student_results SET section = :newSection WHERE sheetTitle = :sheetTitle AND section = :oldSection")
    suspend fun updateSectionName(sheetTitle: String, oldSection: String, newSection: String)

    // Clear section name (set to empty string) for given section of a sheet
    @Query("UPDATE student_results SET section = '' WHERE sheetTitle = :sheetTitle AND section = :sectionName")
    suspend fun clearSectionName(sheetTitle: String, sectionName: String)

    // Optionally, delete all student results under a given section of a sheet
    @Query("DELETE FROM student_results WHERE sheetTitle = :sheetTitle AND section = :sectionName")
    suspend fun deleteResultsBySection(sheetTitle: String, sectionName: String)
}
