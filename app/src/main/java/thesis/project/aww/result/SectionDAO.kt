package thesis.project.aww.result

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDAO {

    @Query("SELECT * FROM sections WHERE sheetTitle = :sheetTitle")
    suspend fun getSectionsBySheetTitle(sheetTitle: String): List<Section>

    @Query("SELECT * FROM sections WHERE sheetTitle = :sheetTitle")
    fun getSectionsBySheetTitleFlow(sheetTitle: String): Flow<List<Section>>

    @Query("SELECT EXISTS(SELECT 1 FROM sections WHERE sheetTitle = :sheetTitle AND name = :sectionName LIMIT 1)")
    suspend fun exists(sheetTitle: String, sectionName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSection(section: Section)

    @Query("UPDATE sections SET name = :newName WHERE sheetTitle = :sheetTitle AND name = :oldName")
    suspend fun renameSection(sheetTitle: String, oldName: String, newName: String)

    @Query("DELETE FROM sections WHERE sheetTitle = :sheetTitle AND name = :sectionName")
    suspend fun deleteSection(sheetTitle: String, sectionName: String)  // <- Ensure suspend here!
}
