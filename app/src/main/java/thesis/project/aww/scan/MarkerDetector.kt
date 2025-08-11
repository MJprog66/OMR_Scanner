package thesis.project.aww.scan

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.*

data class MarkerCorners(
    val topLeft: PointF,
    val topRight: PointF,
    val bottomLeft: PointF,
    val bottomRight: PointF
)

fun detectOmrMarkers(bitmap: Bitmap): MarkerCorners? {
    val mat = Mat()
    Utils.bitmapToMat(bitmap, mat)

    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY)
    Imgproc.GaussianBlur(mat, mat, Size(5.0, 5.0), 0.0)

    Imgproc.adaptiveThreshold(
        mat, mat, 255.0,
        Imgproc.ADAPTIVE_THRESH_MEAN_C,  // updated from GAUSSIAN_C
        Imgproc.THRESH_BINARY_INV,
        11, 1.5  // updated parameters for more reliable binarization
    )

    val contours = mutableListOf<MatOfPoint>()
    val hierarchy = Mat()
    Imgproc.findContours(mat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)

    val detectedCenters = mutableListOf<Point>()

    for (contour in contours) {
        val peri = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
        val approx = MatOfPoint2f()
        Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approx, 0.02 * peri, true)

        if (approx.total() == 4L) {
            val points = approx.toArray()
            val area = Imgproc.contourArea(MatOfPoint(*points.map { Point(it.x, it.y) }.toTypedArray()))
            if (area in 80.0..8000.0) { // expanded from 100..5000 to 80..8000
                val rect = Imgproc.boundingRect(MatOfPoint(*points))
                val aspectRatio = rect.width.toFloat() / rect.height.toFloat()

                if (aspectRatio in 0.7..1.3 && isRoughlyCircular(contour)) {
                    val cx = points.map { it.x }.average()
                    val cy = points.map { it.y }.average()
                    val fillLevel = getBubbleFillLevel(mat, Point(cx, cy), 10)
                    if (fillLevel > 0.25) {
                        detectedCenters.add(Point(cx, cy))
                    }
                }
            }
        }
    }

    if (detectedCenters.size < 4) {
        Log.w("MarkerDetector", "Only detected ${detectedCenters.size} potential markers")
        return null
    }

    val corners = pickBest4Corners(detectedCenters)
    if (corners.size != 4) return null

    val sorted = corners.sortedWith(compareBy({ it.y }, { it.x }))
    val (topTwo, bottomTwo) = sorted.take(2) to sorted.takeLast(2)

    val topLeft = if (topTwo[0].x < topTwo[1].x) topTwo[0] else topTwo[1]
    val topRight = if (topTwo[0].x > topTwo[1].x) topTwo[0] else topTwo[1]
    val bottomLeft = if (bottomTwo[0].x < bottomTwo[1].x) bottomTwo[0] else bottomTwo[1]
    val bottomRight = if (bottomTwo[0].x > bottomTwo[1].x) bottomTwo[0] else bottomTwo[1]

    return MarkerCorners(
        topLeft = PointF(topLeft.x.toFloat(), topLeft.y.toFloat()),
        topRight = PointF(topRight.x.toFloat(), topRight.y.toFloat()),
        bottomLeft = PointF(bottomLeft.x.toFloat(), bottomLeft.y.toFloat()),
        bottomRight = PointF(bottomRight.x.toFloat(), bottomRight.y.toFloat())
    )
}

private fun getBubbleFillLevel(mat: Mat, center: Point, radius: Int): Double {
    val x = max(0, center.x.toInt() - radius)
    val y = max(0, center.y.toInt() - radius)
    val width = min(mat.cols() - x, radius * 2)
    val height = min(mat.rows() - y, radius * 2)

    val roi = mat.submat(y, y + height, x, x + width)
    val nonZero = Core.countNonZero(roi)
    val total = width * height
    return nonZero.toDouble() / total
}

private fun isRoughlyCircular(contour: MatOfPoint): Boolean {
    val area = Imgproc.contourArea(contour)
    val perimeter = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
    if (perimeter == 0.0) return false
    val circularity = 4 * Math.PI * area / (perimeter * perimeter)
    return circularity in 0.6..1.3
}

private fun pickBest4Corners(points: List<Point>): List<Point> {
    if (points.size <= 4) return points

    var bestCombo = listOf<Point>()
    var maxSpread = 0.0

    val combinations = points.combinations(4)
    for (combo in combinations) {
        val spread = combo.sumOf { p1 ->
            combo.sumOf { p2 -> p1.distanceTo(p2) }
        }
        if (spread > maxSpread) {
            maxSpread = spread
            bestCombo = combo
        }
    }
    return bestCombo
}

private fun <T> List<T>.combinations(k: Int): List<List<T>> {
    if (k == 0) return listOf(emptyList())
    if (isEmpty()) return emptyList()
    val head = first()
    val tail = drop(1)
    val withHead = tail.combinations(k - 1).map { listOf(head) + it }
    val withoutHead = tail.combinations(k)
    return withHead + withoutHead
}

private fun Point.distanceTo(other: Point): Double {
    return sqrt((x - other.x).pow(2) + (y - other.y).pow(2))
}

fun warpToTopView(
    bitmap: Bitmap,
    corners: MarkerCorners,
    outputWidth: Int = 595,
    outputHeight: Int = 842
): Bitmap {
    val srcMat = Mat()
    Utils.bitmapToMat(bitmap, srcMat)

    val srcPoints = MatOfPoint2f(
        Point(corners.topLeft.x.toDouble(), corners.topLeft.y.toDouble()),
        Point(corners.topRight.x.toDouble(), corners.topRight.y.toDouble()),
        Point(corners.bottomRight.x.toDouble(), corners.bottomRight.y.toDouble()),
        Point(corners.bottomLeft.x.toDouble(), corners.bottomLeft.y.toDouble())
    )

    val margin = -21.0

    val dstPoints = MatOfPoint2f(
        Point(0.0 - margin, 0.0 - margin),
        Point(outputWidth.toDouble() + margin, 0.0 - margin),
        Point(outputWidth.toDouble() + margin, outputHeight.toDouble() + margin),
        Point(0.0 - margin, outputHeight.toDouble() + margin)
    )

    val transform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
    val warped = Mat()
    Imgproc.warpPerspective(srcMat, warped, transform, Size(outputWidth.toDouble(), outputHeight.toDouble()))

    val result = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(warped, result)

    Log.d("Warp", "✅ Warped image size: ${result.width} x ${result.height}")

    return result
}
