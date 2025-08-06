package thesis.project.aww.util

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

object BitmapUtils {
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
