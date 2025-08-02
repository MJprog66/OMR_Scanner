package thesis.project.aww.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun MainMenu(navController: NavHostController) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Main Menu", fontSize = 28.sp)
            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { navController.navigate("create") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("scan") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("result") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Result")
            }
        }
    }
}