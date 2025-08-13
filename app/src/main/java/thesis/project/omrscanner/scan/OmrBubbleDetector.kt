package thesis.project.omrscanner.scan

import android.graphics.*
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import thesis.project.omrscanner.shared.OmrLayout

object OmrBubbleDetector {

    data class DetectionResult(
        val answers: List<Char?>,
        val debugBitmap: Bitmap
    )

    fun detectFilledBubbles(
        bitmap: Bitmap,
        questionCount: Int,
        answerKey: List<Char?>? = null
    ): DetectionResult {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY)

        Imgproc.GaussianBlur(mat, mat, Size(7.0, 7.0), 2.0)
        Imgproc.adaptiveThreshold(
            mat, mat, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            15, 4.0
        )

        val scaleX = mat.width() / OmrLayout.TEMPLATE_WIDTH
        val scaleY = mat.height() / OmrLayout.TEMPLATE_HEIGHT
        val outputWidth = mat.width()
        val outputHeight = mat.height()

        val answers = mutableListOf<Char?>()
        val debugBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(debugBitmap)

        val defaultPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.GREEN
            isAntiAlias = true
        }

        val incorrectPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.RED
            isAntiAlias = true
        }

        val debugCirclePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            color = Color.LTGRAY
            isAntiAlias = true
        }

        val correctAnswerPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.LTGRAY
            isAntiAlias = true
        }

        val fillThreshold = 0.52

        for (i in 0 until questionCount.coerceAtMost(100)) {
            val column = i / OmrLayout.questionsPerColumn
            val row = i % OmrLayout.questionsPerColumn

            val xOffset = (OmrLayout.startX + column * OmrLayout.colWidth) * scaleX
            val yPos = (OmrLayout.startY + row * OmrLayout.rowHeight) * scaleY

            var selectedChoice: Char? = null
            var selectedCx = 0f
            var selectedCy = 0f
            var maxFillRatio = 0.0

            for (j in OmrLayout.choices.indices) {
                val cx = xOffset + (OmrLayout.bubbleOffsetX + j * (OmrLayout.bubbleSize + OmrLayout.bubbleSpacing)) * scaleX
                val cy = yPos + OmrLayout.bubbleOffsetY * scaleY

                val radius = (OmrLayout.bubbleRadius + 1.5f) * scaleX
                val left = (cx - radius).toInt().coerceIn(0, outputWidth - 1)
                val top = (cy - radius).toInt().coerceIn(0, outputHeight - 1)
                val size = (radius * 2).toInt().coerceAtLeast(1)

                val right = (left + size).coerceAtMost(outputWidth)
                val bottom = (top + size).coerceAtMost(outputHeight)

                val roi = mat.submat(top, bottom, left, right)

                val bin = Mat()
                Imgproc.threshold(roi, bin, 0.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)

                val filledPixels = Core.countNonZero(bin)
                val totalPixels = bin.rows() * bin.cols()
                val fillRatio = filledPixels.toDouble() / totalPixels.toDouble()

                val contours = mutableListOf<MatOfPoint>()
                Imgproc.findContours(bin.clone(), contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

                var isLikelyMark = false
                for (contour in contours) {
                    val rect = Imgproc.boundingRect(contour)
                    val aspectRatio = rect.width.toDouble() / rect.height.toDouble()
                    val area = rect.width * rect.height
                    if (aspectRatio in 0.75..1.3 && area in 70..450) {
                        isLikelyMark = true
                        break
                    }
                }

                canvas.drawCircle(cx.toFloat(), cy.toFloat(), radius, debugCirclePaint)

                bin.release()
                roi.release()

                if (isLikelyMark && fillRatio > fillThreshold && fillRatio > maxFillRatio) {
                    maxFillRatio = fillRatio
                    selectedChoice = OmrLayout.choices[j]
                    selectedCx = cx.toFloat()
                    selectedCy = cy.toFloat()
                }
            }

            answers.add(selectedChoice)

            val correctChoice = answerKey?.getOrNull(i)

            if (selectedChoice != null) {
                val isCorrect = correctChoice?.equals(selectedChoice, ignoreCase = true) ?: true
                canvas.drawCircle(
                    selectedCx,
                    selectedCy,
                    (OmrLayout.bubbleRadius + 4f) * scaleX,
                    if (isCorrect) defaultPaint else incorrectPaint
                )
            } else if (correctChoice != null) {
                val correctIndex = OmrLayout.choices.indexOf(correctChoice.uppercaseChar())
                if (correctIndex != -1) {
                    val cx = xOffset + (OmrLayout.bubbleOffsetX + correctIndex * (OmrLayout.bubbleSize + OmrLayout.bubbleSpacing)) * scaleX
                    val cy = yPos + OmrLayout.bubbleOffsetY * scaleY
                    canvas.drawCircle(
                        cx.toFloat(),
                        cy.toFloat(),
                        (OmrLayout.bubbleRadius + 4f) * scaleX,
                        correctAnswerPaint
                    )
                }
            }
        }

        Log.d("OMR", "Detected answers: $answers")
        return DetectionResult(answers, debugBitmap)
    }
}
