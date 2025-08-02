package thesis.project.aww.result

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import thesis.project.aww.data.AppDatabase

class ResultViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).studentResultDao()

    private val _selectedTitle = MutableStateFlow<String?>(null)

    // Observes results whenever a new title is selected
    val results: StateFlow<List<StudentResult>> = _selectedTitle
        .filterNotNull()
        .flatMapLatest { title ->
            flow {
                emit(dao.getResultsByTitle(title))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertResult(result: StudentResult) {
        viewModelScope.launch {
            dao.insertResult(result)
        }
    }

    fun loadResultsForTitle(title: String) {
        _selectedTitle.value = title
    }

    fun deleteResultsForTitle(title: String) {
        viewModelScope.launch {
            dao.deleteResultsByTitle(title)
            _selectedTitle.value = null
        }
    }

    suspend fun getAllSheetTitles(): List<String> {
        return dao.getAllSheetTitles()
    }
}
