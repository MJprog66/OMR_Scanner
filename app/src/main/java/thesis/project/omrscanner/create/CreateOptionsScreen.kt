package thesis.project.omrscanner.create

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CreateOptionsScreen(
    onCreateClick: () -> Unit,
    onViewClick: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("What would you like to do?", fontSize = 22.sp)
        Spacer(Modifier.height(24.dp))

        Button(onClick = onCreateClick, modifier = Modifier.fillMaxWidth()) {
            Text("Create OMR Sheet")
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = onViewClick, modifier = Modifier.fillMaxWidth()) {
            Text("View Created OMRs")
        }

        Spacer(Modifier.height(32.dp))

        Button(onClick = onBack) {
            Text("Back to Menu")
        }
    }
}
