package thesis.project.omrscanner.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CameraManager {

    suspend fun startCameraPreview(
        context: Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        previewView: PreviewView,
        onCaptureReady: (ImageCapture) -> Unit
    ) {
        withContext(Dispatchers.Main) {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder()
                    .setTargetRotation(previewView.display.rotation)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                Log.d("CameraManager", "✅ Camera preview started")
                onCaptureReady(imageCapture)
            } catch (e: Exception) {
                Log.e("CameraManager", "❌ Failed to start camera preview", e)
                throw e // Re-throw to handle in UI if needed
            }
        }
    }

    fun captureAndProcessImage(
        context: Context,
        imageCapture: ImageCapture?,
        onSuccess: (Bitmap) -> Unit,
        onError: (Exception) -> Unit = { Log.e("CameraManager", "Capture error", it) }
    ) {
        if (imageCapture == null) {
            onError(IllegalStateException("ImageCapture is null"))
            return
        }

        val photoFile = try {
            File.createTempFile("omr_", ".jpg", context.cacheDir).apply { deleteOnExit() }
        } catch (e: Exception) {
            onError(e)
            return
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    try {
                        val rawBitmap = correctImageOrientation(photoFile.absolutePath)
                        val markers = detectOmrMarkers(rawBitmap)

                        if (markers == null) {
                            Log.w("CameraManager", "⚠️ No markers found - using raw image")
                            onSuccess(rawBitmap) // Fallback
                        } else {
                            val warped = warpToTopView(rawBitmap, markers)
                            onSuccess(warped) // Primary path
                        }
                    } catch (e: Exception) {
                        onError(e)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }

    private fun correctImageOrientation(filePath: String): Bitmap {
        val exif = ExifInterface(filePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        val bitmap = BitmapFactory.decodeFile(filePath)
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}