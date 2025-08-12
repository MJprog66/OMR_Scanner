package thesis.project.aww.result

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import thesis.project.aww.data.AppDatabase
import java.io.File

class ResultViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).studentResultDao()
    private val sectionDao = AppDatabase.getDatabase(application).sectionDao()

    // Tracks currently selected sheet title
    private val _selectedSheetTitle = MutableStateFlow<String?>(null)
    val selectedSheetTitle: StateFlow<String?> = _selectedSheetTitle.asStateFlow()

    // Results list updates automatically when selectedSheetTitle changes
    private val _results = MutableStateFlow<List<StudentResult>>(emptyList())
    val results: StateFlow<List<StudentResult>> = _results.asStateFlow()

    // Sections list updates automatically when selectedSheetTitle changes
    val sections: StateFlow<List<String>> = _selectedSheetTitle
        .filterNotNull()
        .flatMapLatest { title ->
            sectionDao.getSectionsBySheetTitleFlow(title)
                .map { sectionList -> sectionList.map { it.name } }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            _selectedSheetTitle
                .filterNotNull()
                .collect { title ->
                    loadResultsForTitleSuspended(title)
                }
        }
    }

    fun setSelectedSheetTitle(title: String) {
        _selectedSheetTitle.value = title
    }

    private suspend fun loadResultsForTitleSuspended(title: String) {
        val resultsFromDb = dao.getResultsByTitle(title)
        _results.value = resultsFromDb
    }

    fun loadResultsForTitle(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            loadResultsForTitleSuspended(title)
        }
    }

    fun insertResult(result: StudentResult) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertResult(result)
            if (_selectedSheetTitle.value == result.sheetTitle) {
                loadResultsForTitleSuspended(result.sheetTitle)
            }
        }
    }

    fun deleteStudentResult(result: StudentResult) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                File(result.image).takeIf { it.exists() }?.delete()
            } catch (_: Exception) {}

            dao.deleteResult(result)

            if (_selectedSheetTitle.value == result.sheetTitle) {
                loadResultsForTitleSuspended(result.sheetTitle)
            }
        }
    }

    fun deleteResultsForTitle(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteResultsByTitle(title)
            if (_selectedSheetTitle.value == title) {
                _results.value = emptyList()
                _selectedSheetTitle.value = null
            }
        }
    }

    suspend fun getAllSheetTitles(): List<String> {
        return dao.getAllSheetTitles()
    }

    // Section-related methods

    fun insertSection(sheetTitle: String, sectionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val exists = sectionDao.exists(sheetTitle, sectionName)
            if (!exists) {
                sectionDao.insertSection(
                    Section(sheetTitle = sheetTitle, name = sectionName)
                )
            }
        }
    }

    fun renameSection(sheetTitle: String, oldName: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            sectionDao.renameSection(sheetTitle, oldName, newName)
            dao.updateSectionName(sheetTitle, oldName, newName)
            // Reload results to reflect changes
            loadResultsForTitleSuspended(sheetTitle)
        }
    }

    fun deleteSection(sheetTitle: String, sectionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            sectionDao.deleteSection(sheetTitle, sectionName)
            dao.clearSectionName(sheetTitle, sectionName)
            // Reload results to reflect changes
            loadResultsForTitleSuspended(sheetTitle)
        }
    }

    fun getResultsGroupedBySection(sheetTitle: String): Map<String, List<StudentResult>> {
        val filteredResults = _results.value.filter { it.sheetTitle == sheetTitle }
        return filteredResults.groupBy { it.section.ifBlank { "No Section" } }
    }
}
