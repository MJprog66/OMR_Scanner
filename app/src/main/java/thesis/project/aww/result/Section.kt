package thesis.project.aww.result

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sections")
data class Section(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val sheetTitle: String,
    val name: String
)
