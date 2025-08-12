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
import kotlinx.coroutines.launch
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
    var sectionName by remember { mutableStateOf("") }
    var showSectionDropdown by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // New state to trigger section add confirmation dialog
    var newSectionName by remember { mutableStateOf("") }
    var showAddSectionConfirmDialog by remember { mutableStateOf(false) }

    val sections by viewModel.sections.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                errorMessage = "Camera permission is required to scan."
                Log.e("ScanScreen", "❌ $errorMessage")
            }
        }
    )

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    withDismissAction = true
                )
            }
            errorMessage = null
        }
    }

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

    LaunchedEffect(selectedSheet) {
        selectedSheet?.let { sheet ->
            viewModel.setSelectedSheetTitle(sheet.title)
            sectionName = ""
            newSectionName = ""
        }
    }

    LaunchedEffect(sections) {
        if (sections.isNotEmpty() && sectionName.isBlank()) {
            sectionName = sections[0]
        }
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp)
                .padding(paddingValues)
        ) {
            // OMR Sheet selector dropdown
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
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
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
                                errorMessage = "❗ Select an OMR sheet before scanning."
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
                                        errorMessage = "❌ Scan failed — please try again."
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
                            onClick = { showNameDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled = selectedSheet != null
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Enter Student Name and Section") },
            text = {
                Column {
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("Student Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = showSectionDropdown,
                        onExpandedChange = { showSectionDropdown = !showSectionDropdown }
                    ) {
                        OutlinedTextField(
                            value = sectionName,
                            onValueChange = { sectionName = it },
                            label = { Text("Section") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSectionDropdown)
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = showSectionDropdown,
                            onDismissRequest = { showSectionDropdown = false },
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                        ) {
                            sections.forEach { section ->
                                DropdownMenuItem(
                                    text = { Text(section) },
                                    onClick = {
                                        sectionName = section
                                        showSectionDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newSectionName,
                            onValueChange = { newSectionName = it },
                            label = { Text("Add New Section") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val trimmed = newSectionName.trim()
                                if (trimmed.isNotEmpty() && selectedSheet != null) {
                                    // Show confirmation dialog before adding
                                    showAddSectionConfirmDialog = true
                                }
                            }
                        ) {
                            Text("Add")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = studentName.isNotBlank() && sectionName.isNotBlank(),
                    onClick = {
                        if (selectedSheet == null) {
                            errorMessage = "❗ Cannot save. No sheet selected."
                            showNameDialog = false
                            return@TextButton
                        }

                        val trimmedSection = sectionName.trim()
                        val trimmedStudentName = studentName.trim()
                        val score = detectedAnswers.zip(selectedSheet!!.answerKey)
                            .count { (given, correct) -> given == correct }
                        val isPass = score >= selectedSheet!!.questionCount / 2
                        val answersString = detectedAnswers.joinToString("") { it?.toString() ?: "-" }

                        val savedImagePath = FileUtils.saveBitmapToInternalStorage(
                            context = context,
                            bitmap = scannedBitmap!!,
                            fileName = "${trimmedStudentName}_${System.currentTimeMillis()}"
                        )

                        viewModel.insertResult(
                            StudentResult(
                                studentName = trimmedStudentName,
                                sheetTitle = selectedSheet!!.title,
                                section = trimmedSection,
                                answers = answersString,
                                score = score,
                                isPass = isPass,
                                image = savedImagePath,
                                totalQuestions = selectedSheet!!.questionCount,
                                timestamp = System.currentTimeMillis()
                            )
                        )

                        // Insert new section if not already in the list
                        if (!sections.contains(trimmedSection)) {
                            viewModel.insertSection(selectedSheet!!.title, trimmedSection)
                        }

                        studentName = ""
                        sectionName = ""
                        newSectionName = ""
                        showNameDialog = false

                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Saved result for $trimmedStudentName")
                        }

                        navigateToResult()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog for adding new section
    if (showAddSectionConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showAddSectionConfirmDialog = false },
            title = { Text("Add New Section") },
            text = { Text("Are you sure you want to add section \"$newSectionName\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newSectionName.trim()
                        if (trimmed.isNotEmpty() && selectedSheet != null) {
                            viewModel.insertSection(selectedSheet!!.title, trimmed)
                            sectionName = trimmed
                            newSectionName = ""
                            showSectionDropdown = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Added new section: $trimmed")
                            }
                        }
                        showAddSectionConfirmDialog = false
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSectionConfirmDialog = false }) {
                    Text("No")
                }
            }
        )
    }
}
