package thesis.project.omrscanner.data

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromCharList(value: List<Char>): String {
        return value.joinToString("") // Convert ['A', 'B', 'C'] → "ABC"
    }

    @TypeConverter
    fun toCharList(value: String): List<Char> {
        return value.toList() // Convert "ABC" → ['A', 'B', 'C']
    }
}