package thesis.project.omrscanner.create

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OmrSheetDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sheet: OmrSheet)

    @Delete
    suspend fun delete(sheet: OmrSheet)

    @Query("SELECT * FROM OmrSheet ORDER BY id DESC")
    fun getAllSheets(): Flow<List<OmrSheet>>

    @Query("SELECT * FROM OmrSheet WHERE id = :sheetId LIMIT 1")
    suspend fun getSheetById(sheetId: Int): OmrSheet?
}