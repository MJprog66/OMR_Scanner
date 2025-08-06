package thesis.project.aww.result

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import java.io.File

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
                ResultViewModel(context.applicationContext as android.app.Application)
            }
        }
    )

    var sheetTitles by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTitle by remember { mutableStateOf(omrSheetTitle) }
    var expanded by remember { mutableStateOf(false) }

    var showDialog by remember { mutableStateOf(false) }
    var dialogBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var resultToDelete by remember { mutableStateOf<StudentResult?>(null) }

    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }

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

                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text("Choose Sheet Title")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        sheetTitles.forEach { title ->
                            DropdownMenuItem(
                                text = { Text(title) },
                                onClick = {
                                    selectedTitle = title
                                    viewModel.loadResultsForTitle(title)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            selectedTitle?.let { title ->
                val results by viewModel.results.collectAsState()

                Text("Results for \"$title\"", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                if (results.isEmpty()) {
                    Text("No results found.")
                } else {
                    LazyColumn {
                        items(results) { result ->
                            val cardColor = if (result.isPass)
                                MaterialTheme.colorScheme.secondaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = cardColor)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Student: ${result.studentName}")
                                            Text("Score: ${result.score}/${result.totalQuestions}")
                                        }
                                        IconButton(onClick = {
                                            resultToDelete = result
                                            showDeleteConfirmDialog = true
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Result"
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    val imageFile = File(result.image)
                                    if (imageFile.exists()) {
                                        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                                        bitmap?.let {
                                            Image(
                                                bitmap = it.asImageBitmap(),
                                                contentDescription = "Scanned Sheet",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(100.dp)
                                                    .clickable {
                                                        dialogBitmap = it
                                                        showDialog = true
                                                    }
                                            )
                                        }
                                    } else {
                                        Text("Image not found", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showDeleteAllConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete All Results for \"$title\"")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                navigateToResult?.let {
                    OutlinedButton(onClick = it) {
                        Text("Back")
                    }
                }
            }
        }
    }

    // Preview Full Image Dialog
    if (showDialog && dialogBitmap != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Close")
                }
            },
            text = {
                Image(
                    bitmap = dialogBitmap!!.asImageBitmap(),
                    contentDescription = "Full Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
            }
        )
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
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
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
                    selectedTitle = null
                    sheetTitles = sheetTitles.filterNot { it == selectedTitle }
                    showDeleteAllConfirmDialog = false
                }) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
