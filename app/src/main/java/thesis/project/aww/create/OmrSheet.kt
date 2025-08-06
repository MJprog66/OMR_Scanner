package thesis.project.aww.create

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity
data class OmrSheet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val questionCount: Int,
    val answerKey: List<Char>,
)
