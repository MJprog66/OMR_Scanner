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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

enum class SortOption(val label: String) { DATE("Date"), NAME("Name"), SCORE("Score") }
enum class FilterOption(val label: String) { ALL("All"), PASS("Pass"), FAIL("Fail") }

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
            initializer { ResultViewModel(context.applicationContext as Application) }
        }
    )

    var sheetTitles by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTitle by remember { mutableStateOf(omrSheetTitle) }
    var selectedSortOption by remember { mutableStateOf(SortOption.DATE) }
    var selectedFilterOption by remember { mutableStateOf(FilterOption.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }

    // Image & Delete states
    var showImageOverlay by remember { mutableStateOf(false) }
    var overlayImagePath by remember { mutableStateOf<String?>(null) }
    var scale by remember { mutableStateOf(1f) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var resultToDelete by remember { mutableStateOf<StudentResult?>(null) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }
    var sheetToDelete by remember { mutableStateOf<String?>(null) }
    var showDeleteSectionDialog by remember { mutableStateOf(false) }
    var sectionToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        sheetTitles = viewModel.getAllSheetTitles()
        selectedTitle?.let { viewModel.loadResultsForTitle(it) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {

            // ======= Main UI =======
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // =================== SHEET LIST ===================
                if (selectedTitle == null) {
                    Text("Select OMR Sheet Title", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search Sheet Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    val filteredTitles = sheetTitles.filter { it.contains(searchQuery, ignoreCase = true) }
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
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        sheetToDelete = title
                                        showDeleteAllConfirmDialog = true
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete All Results")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // =================== RESULTS VIEW ===================
                selectedTitle?.let { title ->
                    val allResults by viewModel.results.collectAsState()
                    val filteredResults = when (selectedFilterOption) {
                        FilterOption.ALL -> allResults
                        FilterOption.PASS -> allResults.filter { it.isPass }
                        FilterOption.FAIL -> allResults.filter { !it.isPass }
                    }
                    val searchedResults = if (searchQuery.isBlank()) filteredResults
                    else filteredResults.filter { it.studentName.contains(searchQuery, ignoreCase = true) }
                    val sortedResults = when (selectedSortOption) {
                        SortOption.DATE -> searchedResults.sortedByDescending { it.timestamp }
                        SortOption.NAME -> searchedResults.sortedBy { it.studentName }
                        SortOption.SCORE -> searchedResults.sortedByDescending { it.score }
                    }
                    val resultsBySection = sortedResults.groupBy { it.section.ifBlank { "No Section" } }

                    Text("Results for \"$title\"", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search by Student Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    // Sort & Filter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        var sortExpanded by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { sortExpanded = true }) { Text("Sort: ${selectedSortOption.label}") }
                        DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(text = { Text(option.label) }, onClick = {
                                    selectedSortOption = option
                                    sortExpanded = false
                                })
                            }
                        }

                        var filterExpanded by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { filterExpanded = true }) { Text("Filter: ${selectedFilterOption.label}") }
                        DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                            FilterOption.values().forEach { option ->
                                DropdownMenuItem(text = { Text(option.label) }, onClick = {
                                    selectedFilterOption = option
                                    filterExpanded = false
                                })
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (sortedResults.isEmpty()) {
                        Text("No results found.")
                    } else {
                        LazyColumn {
                            resultsBySection.forEach { (section, resultsInSection) ->
                                val isExpanded = expandedSections.getOrElse(section) { true }

                                // Section Header
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        elevation = CardDefaults.cardElevation(2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { expandedSections[section] = !isExpanded }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val rotation by animateFloatAsState(if (isExpanded) 180f else 0f)
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.graphicsLayer { rotationZ = rotation })
                                            Spacer(Modifier.width(8.dp))
                                            Text(section, style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.weight(1f))
                                            Text("${resultsInSection.size} results", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            IconButton(onClick = {
                                                sectionToDelete = section
                                                showDeleteSectionDialog = true
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Section", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }

                                if (isExpanded) {
                                    // Table Header
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Student", modifier = Modifier.weight(2f))
                                            Text("Score", modifier = Modifier.weight(1.1f))
                                            Text("Result", modifier = Modifier.weight(1.4f))
                                            Text("Actions", modifier = Modifier.weight(1.3f))
                                        }
                                    }

                                    // Student rows
                                    items(resultsInSection, key = { it.id }) { result ->
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(result.studentName, modifier = Modifier.weight(2f))
                                            Text("${result.score}/${result.totalQuestions}", modifier = Modifier.weight(1f))
                                            Text(if (result.isPass) "Pass" else "Fail",
                                                modifier = Modifier.weight(1f),
                                                color = if (result.isPass) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                            Row(modifier = Modifier.weight(1.5f), horizontalArrangement = Arrangement.End) {
                                                IconButton(onClick = {
                                                    overlayImagePath = result.image
                                                    showImageOverlay = true
                                                    scale = 1f
                                                }) {
                                                    Icon(Icons.Default.Image, contentDescription = "View Image", tint = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(onClick = {
                                                    resultToDelete = result
                                                    showDeleteConfirmDialog = true
                                                }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Result", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =================== IMAGE OVERLAY WITH ZOOM & PAN ===================
            if (showImageOverlay && overlayImagePath != null) {
                var offsetX by remember { mutableStateOf(0f) }
                var offsetY by remember { mutableStateOf(0f) }
                var lastOffsetX by remember { mutableStateOf(0f) }
                var lastOffsetY by remember { mutableStateOf(0f) }
                var scale by remember { mutableStateOf(1f) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                // Zoom
                                scale = (scale * zoom).coerceIn(1f, 5f)

                                // Pan only if zoomed in
                                if (scale > 1f) {
                                    offsetX = (lastOffsetX + pan.x).coerceIn(-1000f, 1000f) // optional max bounds
                                    offsetY = (lastOffsetY + pan.y).coerceIn(-1000f, 1000f)
                                }
                            }
                        }
                ) {
                    AsyncImage(
                        model = overlayImagePath,
                        contentDescription = "Full Image",
                        contentScale = ContentScale.Fit,
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
                        onClick = {
                            showImageOverlay = false
                            // Reset zoom and pan
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                            lastOffsetX = 0f
                            lastOffsetY = 0f
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Fullscreen", tint = Color.White)
                    }
                }

                // Remember last offsets when user stops dragging
                LaunchedEffect(offsetX, offsetY) {
                    lastOffsetX = offsetX
                    lastOffsetY = offsetY
                }
            }

            // =================== DELETE DIALOGS ===================
            if (showDeleteAllConfirmDialog && sheetToDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteAllConfirmDialog = false },
                    title = { Text("Confirm Deletion") },
                    text = { Text("Delete all results of \"$sheetToDelete\"?") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteResultsForTitle(sheetToDelete!!)
                            coroutineScope.launch { snackbarHostState.showSnackbar("Deleted all results for \"$sheetToDelete\"") }
                            sheetTitles = sheetTitles.filterNot { it == sheetToDelete }
                            if (selectedTitle == sheetToDelete) selectedTitle = null
                            sheetToDelete = null
                            showDeleteAllConfirmDialog = false
                        }) { Text("Delete All") }
                    },
                    dismissButton = { TextButton(onClick = { showDeleteAllConfirmDialog = false; sheetToDelete = null }) { Text("Cancel") } }
                )
            }

            if (showDeleteConfirmDialog && resultToDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text("Confirm Deletion") },
                    text = { Text("Delete result for ${resultToDelete!!.studentName}?") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteStudentResult(resultToDelete!!)
                            showDeleteConfirmDialog = false
                            coroutineScope.launch { snackbarHostState.showSnackbar("Deleted result for ${resultToDelete!!.studentName}") }
                        }) { Text("Delete") }
                    },
                    dismissButton = { TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") } }
                )
            }

            if (showDeleteSectionDialog && sectionToDelete != null && selectedTitle != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteSectionDialog = false },
                    title = { Text("Confirm Delete Section") },
                    text = { Text("Delete all results in section \"$sectionToDelete\"?") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteResultsForSection(selectedTitle!!, sectionToDelete!!)
                            coroutineScope.launch { snackbarHostState.showSnackbar("Deleted section \"$sectionToDelete\"") }
                            showDeleteSectionDialog = false
                            sectionToDelete = null
                        }) { Text("Delete") }
                    },
                    dismissButton = { TextButton(onClick = { showDeleteSectionDialog = false; sectionToDelete = null }) { Text("Cancel") } }
                )
            }
        }
    }
}
