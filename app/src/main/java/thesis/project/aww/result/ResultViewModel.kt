package thesis.project.aww.result

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import thesis.project.aww.data.AppDatabase
import thesis.project.aww.result.StudentResult
import java.io.File

class ResultViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).studentResultDao()

    private val _selectedTitle = MutableStateFlow<String?>(null)
    private val _results = MutableStateFlow<List<StudentResult>>(emptyList())
    val results: StateFlow<List<StudentResult>> = _results.asStateFlow()

    // Call this to load results for the selected OMR sheet
    fun loadResultsForTitle(title: String) {
        _selectedTitle.value = title
        viewModelScope.launch {
            _results.value = dao.getResultsByTitle(title)
        }
    }

    // Call this to delete a single result and refresh the list
    fun deleteStudentResult(result: StudentResult) {
        viewModelScope.launch(Dispatchers.IO) {
            // Delete image file from storage if needed (optional)
            try {
                File(result.image).takeIf { it.exists() }?.delete()
            } catch (_: Exception) {}

            dao.deleteResult(result)
            val updatedList = dao.getResultsByTitle(result.sheetTitle)
            _results.value = updatedList
        }
    }

    // Delete all results for a sheet title
    fun deleteResultsForTitle(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteResultsByTitle(title)
            _results.value = emptyList()
            _selectedTitle.value = null
        }
    }

    // List of all available sheet titles with saved results
    suspend fun getAllSheetTitles(): List<String> {
        return dao.getAllSheetTitles()
    }

    // Optional if you're saving new results from somewhere else
    fun insertResult(result: StudentResult) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertResult(result)
        }
    }
}
