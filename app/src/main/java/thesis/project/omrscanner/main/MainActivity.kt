package thesis.project.omrscanner.main

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import org.opencv.android.OpenCVLoader
import thesis.project.omrscanner.ui.theme.ThemeAww

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable Firestore offline persistence
        val db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings
        Log.i("Firestore", "Offline persistence enabled")

        // Initialize OpenCV
        @Suppress("DEPRECATION")
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Initialization failed")
        } else {
            Log.i("OpenCV", "OpenCV initialized successfully")
        }

        // Set Compose UI
        setContent {
            ThemeAww {
                val navController = rememberNavController()
                AppNavigation(navController)
            }
        }
    }
}
