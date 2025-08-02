package thesis.project.aww.create

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import java.io.File

@Composable
fun PortraitOMRConfigScreen(
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("OMR Sheet") }
    var questionCount by remember { mutableIntStateOf(10) }
    var questionInput by remember { mutableStateOf("10") }

    val answerKey = remember { mutableStateListOf<Char>() }

    // Keep answerKey in sync with question count
    LaunchedEffect(questionCount) {
        while (answerKey.size < questionCount) {
            answerKey.add('A')
        }
        while (answerKey.size > questionCount) {
            answerKey.removeAt(answerKey.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Configure OMR (Portrait)", fontSize = 22.sp)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("OMR Title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = questionInput,
                onValueChange = {
                    if (it.length <= 3 && it.all(Char::isDigit)) {
                        questionInput = it
                        val parsed = it.toIntOrNull()
                        if (parsed != null && parsed in 1..100) {
                            questionCount = parsed
                        }
                    }
                },
                label = { Text("Number of Questions (1–100)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Answer Key:")

            Column(modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until questionCount) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text("Q${i + 1}", modifier = Modifier.width(40.dp))
                        listOf('A', 'B', 'C', 'D').forEach { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                RadioButton(
                                    selected = answerKey.getOrNull(i) == option,
                                    onClick = {
                                        if (i < answerKey.size) {
                                            answerKey[i] = option
                                        }
                                    }
                                )
                                Text(option.toString())
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 🎨 Redesigned button row
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = {
                    saveOmrSheet(context, title, questionCount, answerKey.toList())
                    onSave()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }

            OutlinedButton(
                onClick = {
                    printOmrSheet(context, title, questionCount)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Print")
            }
        }

    }
}

private fun saveOmrSheet(
    context: Context,
    title: String,
    questionCount: Int,
    answerKey: List<Char>
) {
    try {
        val omrSheet = OmrSheet(
            title = title.trim(),
            questionCount = questionCount,
            answerKey = answerKey
        )
        val gson = Gson()
        val json = gson.toJson(omrSheet)
        val fileName = generateUniqueOmrFileName(context, title)
        val file = File(context.filesDir, fileName)
        file.writeText(json)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun generateUniqueOmrFileName(context: Context, baseTitle: String): String {
    val sanitizedTitle = baseTitle.trim().replace("\\s+".toRegex(), "_")
    var fileName = "$sanitizedTitle.omr.json"
    var file = File(context.filesDir, fileName)
    var counter = 1

    while (file.exists()) {
        fileName = "${sanitizedTitle}($counter).omr.json"
        file = File(context.filesDir, fileName)
        counter++
    }

    return fileName
}
