package thesis.project.aww.result

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StudentResultDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: StudentResult)

    @Query("SELECT * FROM student_results WHERE omrSheetTitle = :title ORDER BY id DESC")
    suspend fun getResultsByTitle(title: String): List<StudentResult>

    @Query("DELETE FROM student_results WHERE omrSheetTitle = :title")
    suspend fun deleteResultsByTitle(title: String)

    @Query("SELECT DISTINCT omrSheetTitle FROM student_results")
    suspend fun getAllSheetTitles(): List<String>
}
