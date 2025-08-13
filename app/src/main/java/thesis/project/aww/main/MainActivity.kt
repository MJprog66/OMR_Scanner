package thesis.project.aww.main

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import org.opencv.android.OpenCVLoader
import thesis.project.aww.ui.theme.ThemeAww  // Import your theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Initialization failed")
        } else {
            Log.i("OpenCV", "OpenCV initialized successfully")
        }

        setContent {
            ThemeAww {  // Wrap content with your Compose MaterialTheme
                val navController = rememberNavController()
                AppNavigation(navController)
            }
        }
    }
}
