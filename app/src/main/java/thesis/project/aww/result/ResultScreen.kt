package thesis.project.aww.result

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController

@Composable
fun ResultScreen(
    navigateToResult: (() -> Unit)? = null,
    omrSheetTitle: String? = null,
    totalQuestions: Int? = null
) {
    val context = LocalContext.current
    val viewModel: ResultViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ResultViewModel(context.applicationContext as android.app.Application)
            }
        }
    )

    var sheetTitles by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTitle by remember { mutableStateOf(omrSheetTitle) }

    LaunchedEffect(Unit) {
        sheetTitles = viewModel.getAllSheetTitles()
        selectedTitle?.let { viewModel.loadResultsForTitle(it) }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        if (selectedTitle == null) {
            Text("Select OMR Sheet Title", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            var expanded by remember { mutableStateOf(false) }

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
                        val isPassed = result.score >= result.totalQuestions / 2
                        val cardColor = if (isPassed)
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
                                Text("Student: ${result.studentName}")
                                Text("Score: ${result.score}/${result.totalQuestions}")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.deleteResultsForTitle(title)
                        sheetTitles = sheetTitles.filterNot { it == title }
                        selectedTitle = null
                    },
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