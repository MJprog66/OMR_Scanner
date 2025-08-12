package thesis.project.aww.result

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_results")
data class StudentResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentName: String,
    val sheetTitle: String,
    val section: String, // keep as non-null if section is always required
    val totalQuestions: Int,
    val answers: String, // Store answers as comma-separated String like "A,B,C,null"
    val score: Int,
    val isPass: Boolean,
    val image: String,
    val timestamp: Long = System.currentTimeMillis()
)
