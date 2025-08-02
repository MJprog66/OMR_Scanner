package thesis.project.aww.create

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

@Composable
fun ViewCreatedOMRsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val gson = remember { Gson() }
    val omrSheets = remember { mutableStateListOf<OmrSheet>() }
    val fileMap = remember { mutableStateMapOf<OmrSheet, File>() }
    var sheetToDelete by remember { mutableStateOf<OmrSheet?>(null) }
    var sheetToEdit by remember { mutableStateOf<Pair<OmrSheet, File>?>(null) }

    fun loadOmrSheets() {
        omrSheets.clear()
        fileMap.clear()

        val files = context.filesDir
            .listFiles { file ->
                file.extension == "json" && file.name.endsWith(".omr.json")
            }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        for (file in files) {
            try {
                val json = file.readText()
                val sheet: OmrSheet = gson.fromJson(json, object : TypeToken<OmrSheet>() {}.type)
                omrSheets.add(sheet)
                fileMap[sheet] = file
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        loadOmrSheets()
    }

    sheetToEdit?.let { (sheet, file) ->
        EditOMRScreen(
            originalSheet = sheet,
            originalFile = file,
            onSave = {
                sheetToEdit = null
                loadOmrSheets()
            },
            onCancel = { sheetToEdit = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Saved OMR Sheets", fontSize = 22.sp)
        Spacer(Modifier.height(24.dp))

        if (omrSheets.isEmpty()) {
            Text("No OMRs saved yet.")
        } else {
            omrSheets.forEach { sheet ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(sheet.title, fontSize = 18.sp)
                        Text("Questions: ${sheet.questionCount}")
                        Text("Answer Key: ${formatAnswerKey(sheet.answerKey)}")
                        Spacer(Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(onClick = {
                                fileMap[sheet]?.let { file ->
                                    sheetToEdit = sheet to file
                                }
                            }) {
                                Text("Edit")
                            }

                            OutlinedButton(onClick = {
                                printOmrSheet(context, sheet.title, sheet.questionCount)
                            }) {
                                Text("Print")
                            }

                            OutlinedButton(onClick = {
                                // Placeholder for scan functionality
                            }) {
                                Text("Scan")
                            }

                            Button(
                                onClick = { sheetToDelete = sheet },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text("Del")
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }

    sheetToDelete?.let { sheet ->
        AlertDialog(
            onDismissRequest = { sheetToDelete = null },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete '${sheet.title}'?") },
            confirmButton = {
                TextButton(onClick = {
                    fileMap[sheet]?.delete()
                    omrSheets.remove(sheet)
                    fileMap.remove(sheet)
                    sheetToDelete = null
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { sheetToDelete = null }) {
                    Text("No")
                }
            }
        )
    }
}

private fun formatAnswerKey(answerKey: List<Char>): String {
    return if (answerKey.size > 10) {
        answerKey.take(10).joinToString(", ") + ", ..."
    } else {
        answerKey.joinToString(", ")
    }
}
