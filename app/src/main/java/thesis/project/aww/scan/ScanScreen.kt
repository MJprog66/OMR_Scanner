package thesis.project.aww.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import thesis.project.aww.create.OmrSheet
import thesis.project.aww.result.ResultViewModel
import thesis.project.aww.result.StudentResult
import thesis.project.aww.util.FileUtils
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(navigateToResult: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val gson = remember { Gson() }

    val viewModel: ResultViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ResultViewModel(context.applicationContext as android.app.Application)
            }
        }
    )

    val omrSheets = remember { mutableStateListOf<OmrSheet>() }
    var selectedSheet by remember { mutableStateOf<OmrSheet?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var scannedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detectedAnswers by remember { mutableStateOf<List<Char?>>(emptyList()) }
    var showNameDialog by remember { mutableStateOf(false) }
    var studentName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                errorMessage = "Camera permission denied"
                Log.e("ScanScreen", "❌ $errorMessage")
            }
        }
    )

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        val files = context.filesDir.listFiles()?.filter { it.extension == "json" } ?: emptyList()
        omrSheets.addAll(files.mapNotNull { file ->
            try {
                gson.fromJson(file.readText(), OmrSheet::class.java)
            } catch (e: Exception) {
                Log.e("ScanScreen", "Failed to load sheet: ${file.name}", e)
                null
            }
        })
    }

    LaunchedEffect(previewView) {
        if (previewView != null) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                CameraManager.startCameraPreview(
                    context = context,
                    previewView = previewView!!,
                    lifecycleOwner = lifecycleOwner,
                    onCaptureReady = { capture -> imageCapture = capture }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp)
    ) {
        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Dropdown
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = selectedSheet?.title ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select OMR Sheet") },
                    trailingIcon = {
                        Row {
                            if (selectedSheet != null) {
                                IconButton(onClick = { selectedSheet = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(text = { Text("None") }, onClick = {
                        selectedSheet = null
                        expanded = false
                    })
                    omrSheets.forEach { sheet ->
                        DropdownMenuItem(
                            text = { Text(sheet.title) },
                            onClick = {
                                selectedSheet = sheet
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (scannedBitmap == null) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }.also { previewView = it }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    bitmap = scannedBitmap!!.asImageBitmap(),
                    contentDescription = "Scanned Sheet",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            if (scannedBitmap == null) {
                Button(
                    onClick = {
                        if (selectedSheet == null) {
                            errorMessage = "❗ Please select an OMR sheet before scanning."
                        } else {
                            errorMessage = null
                            CameraManager.captureAndProcessImage(
                                context = context,
                                imageCapture = imageCapture,
                                onSuccess = { warpedBitmap ->
                                    val result = OmrBubbleDetector.detectFilledBubbles(
                                        bitmap = warpedBitmap,
                                        questionCount = selectedSheet!!.questionCount,
                                        answerKey = selectedSheet!!.answerKey
                                    )
                                    scannedBitmap = result.debugBitmap
                                    detectedAnswers = result.answers
                                },
                                onError = {
                                    errorMessage = "❌ Scan failed. Please try again."
                                    Log.e("ScanScreen", errorMessage!!)
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scan OMR Sheet")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            scannedBitmap = null
                            detectedAnswers = emptyList()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Scan Again")
                    }
                    Button(
                        onClick = {
                            showNameDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    if (selectedSheet == null) {
                        errorMessage = "❗ Cannot save. No OMR sheet selected."
                        showNameDialog = false
                        return@TextButton
                    }
                    if (studentName.isBlank()) {
                        errorMessage = "❗ Student name cannot be empty."
                        showNameDialog = false
                        return@TextButton
                    }

                    val score = detectedAnswers.zip(selectedSheet!!.answerKey)
                        .count { (given, correct) -> given == correct }

                    val isPass = score >= selectedSheet!!.questionCount / 2
                    val answersString = detectedAnswers.joinToString("") { it?.toString() ?: "-" }

                    viewModel.insertResult(
                        StudentResult(
                            studentName = studentName,
                            omrSheetTitle = selectedSheet!!.title,
                            answers = answersString,
                            score = score,
                            isPass = isPass,
                            image = FileUtils.saveBitmapToInternalStorage(
                                context = context,
                                bitmap = scannedBitmap!!,
                                fileName = "${studentName}_${System.currentTimeMillis()}"
                            ),
                            totalQuestions = selectedSheet!!.questionCount,
                            timestamp = System.currentTimeMillis()
                        )
                    )

                    studentName = ""
                    showNameDialog = false
                    navigateToResult()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Enter Student Name") },
            text = {
                OutlinedTextField(
                    value = studentName,
                    onValueChange = { studentName = it },
                    label = { Text("Student Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}
