package thesis.project.omrscanner.result

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import kotlinx.coroutines.launch


enum class SortOption(val label: String) {
    DATE("Date"),
    NAME("Name"),
    SCORE("Score")
}

enum class FilterOption(val label: String) {
    ALL("All"),
    PASS("Pass"),
    FAIL("Fail")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    navigateToResult: (() -> Unit)? = null,
    omrSheetTitle: String? = null
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val viewModel: ResultViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ResultViewModel(context.applicationContext as Application)
            }
        }
    )

    var sheetTitles by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTitle by remember { mutableStateOf(omrSheetTitle) }

    // Sort/filter/search states
    var selectedSortOption by remember { mutableStateOf(SortOption.DATE) }
    var selectedFilterOption by remember { mutableStateOf(FilterOption.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    // Sections expanded state
    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }

    // Dialog states
    var showDialog by remember { mutableStateOf(false) }
    var dialogImagePath by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var resultToDelete by remember { mutableStateOf<StudentResult?>(null) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }
    var sectionToDelete by remember { mutableStateOf<String?>(null) }
    var showDeleteSectionDialog by remember { mutableStateOf(false) }
    var sheetToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        sheetTitles = viewModel.getAllSheetTitles()
        selectedTitle?.let { viewModel.loadResultsForTitle(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(16.dp).padding(padding)) {
            if (selectedTitle == null) {
                Text("Select OMR Sheet Title", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // 🔍 Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Sheet Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 📄 Sheet Title Cards
                val filteredTitles = sheetTitles.filter {
                    it.contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredTitles) { title ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTitle = title
                                    viewModel.loadResultsForTitle(title)
                                    searchQuery = ""
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    sheetToDelete = title
                                    showDeleteAllConfirmDialog = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete All Results"
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 👉 Existing results display when a title is selected
            selectedTitle?.let { title ->
                val allResults by viewModel.results.collectAsState()

                val filteredResults = remember(allResults, selectedFilterOption) {
                    when (selectedFilterOption) {
                        FilterOption.ALL -> allResults
                        FilterOption.PASS -> allResults.filter { it.isPass }
                        FilterOption.FAIL -> allResults.filter { !it.isPass }
                    }
                }

                val searchedResults = remember(filteredResults, searchQuery) {
                    if (searchQuery.isBlank()) filteredResults
                    else filteredResults.filter {
                        it.studentName.contains(searchQuery, ignoreCase = true)
                    }
                }

                val sortedResults = remember(searchedResults, selectedSortOption) {
                    when (selectedSortOption) {
                        SortOption.DATE -> searchedResults.sortedByDescending { it.timestamp }
                        SortOption.NAME -> searchedResults.sortedBy { it.studentName }
                        SortOption.SCORE -> searchedResults.sortedByDescending { it.score }
                    }
                }

                val resultsBySection = remember(sortedResults) {
                    sortedResults.groupBy { it.section.ifBlank { "No Section" } }
                }

                Text("Results for \"$title\"", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                // 🔍 Search within students
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by Student Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Existing Sort & Filter Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var sortExpanded by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { sortExpanded = true }) {
                        Text("Sort: ${selectedSortOption.label}")
                    }
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        SortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    selectedSortOption = option
                                    sortExpanded = false
                                }
                            )
                        }
                    }

                    var filterExpanded by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { filterExpanded = true }) {
                        Text("Filter: ${selectedFilterOption.label}")
                    }
                    DropdownMenu(
                        expanded = filterExpanded,
                        onDismissRequest = { filterExpanded = false }
                    ) {
                        FilterOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    selectedFilterOption = option
                                    filterExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 👉 Existing results list
                if (sortedResults.isEmpty()) {
                    Text("No results found.")
                } else {
                    LazyColumn {
                        resultsBySection.forEach { (section, resultsInSection) ->
                            val isExpanded = expandedSections.getOrElse(section) { true }

                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedSections[section] = !isExpanded
                                        }
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        modifier = Modifier.graphicsLayer { rotationZ = rotation }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = section,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.weight(1f))

                                    Text("${resultsInSection.size} result(s)")

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = {
                                            sectionToDelete = section
                                            showDeleteSectionDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Section"
                                        )
                                    }
                                }
                            }

                            if (isExpanded) {
                                // Table header (once per section)
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(vertical = 8.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Student",
                                            modifier = Modifier.weight(2f),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            "Score",
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            "Result",
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Spacer(modifier = Modifier.width(48.dp)) // delete column
                                    }
                                }

                                items(resultsInSection) { result ->
                                    val rowColor = if (result.isPass)
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                    else
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(rowColor)
                                            .padding(vertical = 8.dp, horizontal = 12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(result.studentName, modifier = Modifier.weight(2f))
                                            Text(
                                                "${result.score}/${result.totalQuestions}",
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                if (result.isPass) "Pass" else "Fail",
                                                modifier = Modifier.weight(1f)
                                            )

                                            IconButton(onClick = {
                                                resultToDelete = result
                                                showDeleteConfirmDialog = true
                                            }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete Result"
                                                )
                                            }
                                        }

                                        // Image preview
                                        AsyncImage(
                                            model = result.image,
                                            contentDescription = "Scanned Sheet",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp)
                                                .clickable {
                                                    dialogImagePath = result.image
                                                    showDialog = true
                                                }
                                                .padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = { selectedTitle = null }) {
            Text("Back to Sheets") }

        /**------------DIALOGS-------------**/
        // Delete All Confirmation for a Sheet
        if (showDeleteAllConfirmDialog && sheetToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteAllConfirmDialog = false },
                title = { Text("Confirm Deletion") },
                text = { Text("Delete all results of \"${sheetToDelete}\"?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteResultsForTitle(sheetToDelete!!)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Deleted all results for \"$sheetToDelete\"")
                        }
                        sheetTitles =
                            sheetTitles.filterNot { it == sheetToDelete }
                        if (selectedTitle == sheetToDelete) selectedTitle = null
                        sheetToDelete = null
                        showDeleteAllConfirmDialog = false
                    }) {
                        Text("Delete All")
                    } },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteAllConfirmDialog = false
                        sheetToDelete = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Fullscreen Zoomable Image Viewer
        if (showDialog && dialogImagePath != null) {
            var scale by remember { mutableStateOf(1f) }
            var offsetX by remember { mutableStateOf(0f) }
            var offsetY by remember { mutableStateOf(0f) }
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                color = Color.Black
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        }
                ) {
                    AsyncImage(
                        model = dialogImagePath,
                        contentDescription = "Full Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                    IconButton(
                        onClick = { showDialog = false },
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopEnd)
                            .size(36.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Fullscreen",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Single Delete Confirmation Dialog
        if (showDeleteConfirmDialog && resultToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Confirm Deletion") },
                text = { Text("Are you sure you want to delete result for ${resultToDelete!!.studentName}?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteStudentResult(resultToDelete!!)
                        showDeleteConfirmDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Deleted result for ${resultToDelete!!.studentName}")
                        }
                    }) {
                        Text("Delete")
                    } },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete Section Confirmation Dialog
        if (showDeleteSectionDialog && sectionToDelete != null && selectedTitle != null) {
            AlertDialog(
                onDismissRequest = { showDeleteSectionDialog = false },
                title = { Text("Confirm Delete Section") },
                text = { Text("Are you sure you want to delete all results in section \"$sectionToDelete\"?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteResultsForSection(
                            selectedTitle!!,
                            sectionToDelete!!
                        )
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Deleted section \"$sectionToDelete\"")
                        }
                        showDeleteSectionDialog = false
                        sectionToDelete = null
                    }) {
                        Text("Delete")
                    } },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteSectionDialog = false
                        sectionToDelete = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete All Results Confirmation
        if (showDeleteAllConfirmDialog && selectedTitle != null) {
            AlertDialog(
                onDismissRequest = { showDeleteAllConfirmDialog = false },
                title = { Text("Confirm Deletion") },
                text = { Text("Are you sure you want to delete all results of \"$selectedTitle\"?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteResultsForTitle(selectedTitle!!)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Deleted all results for \"$selectedTitle\"")
                        }
                        val removedTitle = selectedTitle
                        selectedTitle = null
                        sheetTitles =
                            sheetTitles.filterNot { it == removedTitle }
                        showDeleteAllConfirmDialog = false
                    }) {
                        Text("Delete All")
                    } },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteAllConfirmDialog = false
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}