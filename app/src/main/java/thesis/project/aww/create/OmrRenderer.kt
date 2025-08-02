package thesis.project.aww.create

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.*
import android.print.*
import android.print.pdf.PrintedPdfDocument
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.FileOutputStream

fun printOmrSheet(context: Context, title: String, questionCount: Int) {
    try {
        val file = File(context.cacheDir, "omr_template_base.pdf")
        if (!file.exists()) {
            context.assets.open("omr_template_base.pdf").use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }

        val pdfDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pdfDescriptor)
        val sourcePage = renderer.openPage(0)

        val scaleFactor = 4
        val width = sourcePage.width * scaleFactor
        val height = sourcePage.height * scaleFactor
        val bitmap = createBitmap(width, height)

        val destRect = Rect(0, 0, width, height)
        sourcePage.render(bitmap, destRect, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
        sourcePage.close()
        renderer.close()

        val canvasOnBitmap = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            typeface = Typeface.MONOSPACE // ensures consistent number width
        }

        // Title
        paint.style = Paint.Style.FILL
        paint.textSize = 22f * scaleFactor
        paint.textAlign = Paint.Align.CENTER
        canvasOnBitmap.drawText(
            title,
            (47f + 501.64f / 2f) * scaleFactor,
            (44f + 22f) * scaleFactor,
            paint
        )

        // Name label and line
        paint.style = Paint.Style.FILL
        paint.textSize = 14f * scaleFactor
        paint.textAlign = Paint.Align.LEFT
        canvasOnBitmap.drawText("Name :", 50f * scaleFactor, 100f * scaleFactor, paint)
        canvasOnBitmap.drawLine(
            100f * scaleFactor, 105f * scaleFactor,
            (100f + 249f) * scaleFactor, 105f * scaleFactor, paint
        )

        // Bubbles
        val bubbleSize = 8f * scaleFactor
        val bubbleRadius = bubbleSize / 2f
        val bubbleSpacing = 9f * scaleFactor
        val rowHeight = 24f * scaleFactor
        val colWidth = 130f * scaleFactor
        val startX = 50f * scaleFactor
        val startY = 140f * scaleFactor
        val questionsPerColumn = 25

        for (i in 0 until questionCount.coerceAtMost(100)) {
            val column = i / questionsPerColumn
            val row = i % questionsPerColumn

            val xOffset = (startX + column * colWidth).toInt()
            val yRowCenter = (startY + row * rowHeight).toInt()

            val numberTextWidth = 25f * scaleFactor
            val bubbleStartX = (xOffset + numberTextWidth).toInt()

            // Question number
            paint.style = Paint.Style.FILL
            paint.textSize = 8f * scaleFactor
            paint.textAlign = Paint.Align.LEFT
            canvasOnBitmap.drawText("${i + 1}.", xOffset.toFloat(), yRowCenter + (bubbleRadius * 0.5f), paint)

            // Answer bubbles
            val bubbleSpacingExact = (2 * bubbleRadius + bubbleSpacing).toInt()
            for (j in 0..3) {
                val cx = bubbleStartX + (j * bubbleSpacingExact)
                val cy = yRowCenter
                paint.style = Paint.Style.STROKE
                canvasOnBitmap.drawCircle(cx.toFloat(), cy.toFloat(), bubbleRadius, paint)
            }
        }

        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        printManager.print("$title OMR", object : PrintDocumentAdapter() {
            var document: PrintedPdfDocument? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                val noMarginAttributes = PrintAttributes.Builder()
                    .setMediaSize(newAttributes.mediaSize ?: PrintAttributes.MediaSize.NA_LETTER)
                    .setResolution(newAttributes.resolution ?: PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()

                document = PrintedPdfDocument(context, noMarginAttributes)

                val info = PrintDocumentInfo.Builder("$title.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal,
                callback: WriteResultCallback
            ) {
                document?.let {
                    val printPage = it.startPage(0)
                    val canvasOnPage = printPage.canvas

                    val scale = minOf(
                        canvasOnPage.width / bitmap.width.toFloat(),
                        canvasOnPage.height / bitmap.height.toFloat()
                    )
                    val dx = (canvasOnPage.width - bitmap.width * scale) / 2f
                    val dy = (canvasOnPage.height - bitmap.height * scale) / 2f

                    val matrix = Matrix().apply {
                        postScale(scale, scale)
                        postTranslate(dx, dy)
                    }

                    canvasOnPage.drawBitmap(bitmap, matrix, null)
                    it.finishPage(printPage)

                    try {
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            it.writeTo(output)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        it.close()
                    }

                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                }
            }
        }, null)

    } catch (e: Exception) {
        e.printStackTrace()
    }
}
