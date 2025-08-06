package thesis.project.aww.result

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

    @Delete
    suspend fun deleteResult(result: StudentResult): Int

    @Query("DELETE FROM student_results WHERE sheetTitle = :title")

    suspend fun deleteResultsByTitle(title: String)

    @Query("SELECT DISTINCT sheetTitle FROM student_results")
    suspend fun getAllSheetTitles(): List<String>
}
