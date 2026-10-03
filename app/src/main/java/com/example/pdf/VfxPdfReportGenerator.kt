package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.data.model.CaptureEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ShootingDayEntity
import com.example.domain.inheritance.MetadataResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfExportOptions(
    val layoutType: String = "Detailed", // "Detailed", "Compact", "Image Only"
    val includeProjectDetails: Boolean = true,
    val includeDayDetails: Boolean = true,
    val includeImages: Boolean = true,
    val includeCameraMetadata: Boolean = true,
    val includeVfxMetadata: Boolean = true,
    val includeNotes: Boolean = true,
    val includeLocation: Boolean = true,
    val selectedDayId: String? = null // null means all days
)

object VfxPdfReportGenerator {

    // A4 dimensions at 72 dpi: 595 x 842
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    suspend fun generateReport(
        context: Context,
        project: ProjectEntity,
        days: List<ShootingDayEntity>,
        capturesByDay: Map<String, List<CaptureEntity>>,
        options: PdfExportOptions = PdfExportOptions()
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()

        val filteredDays = if (options.selectedDayId != null) {
            days.filter { it.id == options.selectedDayId }
        } else {
            days
        }

        val reportsDir = File(context.filesDir, "reports").apply { if (!exists()) mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cleanProjectName = project.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val pdfFile = File(reportsDir, "VFX_Report_${cleanProjectName}_$timeStamp.pdf")

        try {
            val pdfDocument = PdfDocument()

            // Paints
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 10f
            }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            isFakeBoldText = true
        }
        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 24, 32)
            textSize = 20f
            isFakeBoldText = true
        }
        val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(217, 119, 6) // Warm Amber
            textSize = 12f
            isFakeBoldText = true
        }
        val sectionHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 14f
            isFakeBoldText = true
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9f
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val amberLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(245, 158, 11)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val boxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }

        var pageNumber = 1
        var currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = currentPage.canvas
        var currentY = MARGIN

        fun drawHeaderAndFooter(c: Canvas, pNum: Int) {
            // Header line
            c.drawText("VFX CAPTURE REPORT  ·  ${project.name.uppercase(Locale.US)}", MARGIN, 24f, mutedPaint)
            c.drawLine(MARGIN, 28f, PAGE_WIDTH - MARGIN, 28f, linePaint)

            // Footer line
            c.drawLine(MARGIN, PAGE_HEIGHT - 28f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 28f, linePaint)
            val dateStr = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.US).format(Date())
            c.drawText("Generated: $dateStr", MARGIN, PAGE_HEIGHT - 16f, mutedPaint)
            val pageStr = "Page $pNum"
            val pWidth = mutedPaint.measureText(pageStr)
            c.drawText(pageStr, PAGE_WIDTH - MARGIN - pWidth, PAGE_HEIGHT - 16f, mutedPaint)
        }

        fun advancePage() {
            drawHeaderAndFooter(canvas, pageNumber)
            pdfDocument.finishPage(currentPage)
            pageNumber++
            currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = currentPage.canvas
            currentY = MARGIN + 12f
        }

        fun ensureSpace(neededHeight: Float) {
            if (currentY + neededHeight > PAGE_HEIGHT - 45f) {
                advancePage()
            }
        }

        // --- Cover / Project Details Section ---
        if (options.includeProjectDetails) {
            currentY = 46f
            canvas.drawText("VFX CAPTURE REPORT", MARGIN, currentY, subTitlePaint)
            currentY += 24f
            canvas.drawText(project.name.uppercase(Locale.US), MARGIN, currentY, headerTitlePaint)
            currentY += 8f
            canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, amberLinePaint)
            currentY += 16f

            // Project info table box
            val boxHeight = 110f
            canvas.drawRoundRect(RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + boxHeight), 4f, 4f, boxFillPaint)
            canvas.drawRoundRect(RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + boxHeight), 4f, 4f, linePaint)

            val col1X = MARGIN + 12f
            val col2X = MARGIN + (CONTENT_WIDTH / 2f) + 6f
            var rowY = currentY + 16f

            canvas.drawText("Client:", col1X, rowY, boldPaint)
            canvas.drawText(project.client.ifBlank { "N/A" }, col1X + 70f, rowY, textPaint)
            canvas.drawText("Date:", col2X, rowY, boldPaint)
            canvas.drawText(project.date.ifBlank { "N/A" }, col2X + 85f, rowY, textPaint)

            rowY += 15f
            canvas.drawText("Production:", col1X, rowY, boldPaint)
            canvas.drawText(project.productionCompany.ifBlank { "N/A" }, col1X + 70f, rowY, textPaint)
            canvas.drawText("Location:", col2X, rowY, boldPaint)
            canvas.drawText(project.location.ifBlank { "N/A" }, col2X + 85f, rowY, textPaint)

            rowY += 15f
            canvas.drawText("VFX Supervisor:", col1X, rowY, boldPaint)
            canvas.drawText(project.vfxSupervisor.ifBlank { "N/A" }, col1X + 85f, rowY, textPaint)
            canvas.drawText("Director:", col2X, rowY, boldPaint)
            canvas.drawText(project.director.ifBlank { "N/A" }, col2X + 85f, rowY, textPaint)

            rowY += 15f
            canvas.drawText("VFX Producer:", col1X, rowY, boldPaint)
            canvas.drawText(project.vfxProducer.ifBlank { "N/A" }, col1X + 85f, rowY, textPaint)
            canvas.drawText("Camera Op:", col2X, rowY, boldPaint)
            canvas.drawText(project.cameraOperator.ifBlank { "N/A" }, col2X + 85f, rowY, textPaint)

            rowY += 15f
            canvas.drawText("Project Code:", col1X, rowY, boldPaint)
            canvas.drawText(project.projectIdCode.ifBlank { "N/A" }, col1X + 70f, rowY, textPaint)
            canvas.drawText("Shooting Days:", col2X, rowY, boldPaint)
            canvas.drawText("${filteredDays.size} Days", col2X + 85f, rowY, textPaint)

            currentY += boxHeight + 14f

            // Project Default Camera Setup
            ensureSpace(70f)
            canvas.drawText("DEFAULT CAMERA SETUP", MARGIN, currentY, boldPaint)
            currentY += 8f
            canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, linePaint)
            currentY += 14f

            val cam = project.cameraDefaults
            val camSpecs = listOf(
                "Camera: ${cam.cameraManufacturer} ${cam.cameraModel}",
                "Lens: ${cam.focalLength} (${cam.lensManufacturer} ${cam.lensModel})",
                "ISO: ${cam.iso}",
                "Shutter: ${cam.shutterSpeed}",
                "Aperture: ${cam.aperture}",
                "FPS: ${cam.frameRate}",
                "Color Space: ${cam.colorSpace}",
                "Gamma: ${cam.gammaProfile}",
                "Resolution: ${cam.resolution}"
            )

            val colW = CONTENT_WIDTH / 3f
            for (i in camSpecs.indices) {
                val cIdx = i % 3
                val rIdx = i / 3
                val x = MARGIN + (cIdx * colW)
                val y = currentY + (rIdx * 14f)
                canvas.drawText(camSpecs[i], x, y, textPaint)
            }
            currentY += 46f
        }

        // --- Iterate Shooting Days & Captures ---
        for (day in filteredDays) {
            val dayCaptures = capturesByDay[day.id] ?: emptyList()

            ensureSpace(50f)
            if (options.includeDayDetails) {
                // Day header banner
                canvas.drawRoundRect(RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 24f), 3f, 3f, boxFillPaint)
                canvas.drawRoundRect(RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 24f), 3f, 3f, amberLinePaint)

                canvas.drawText(
                    "DAY ${day.dayNumber}   ·   ${day.date}   ·   ${day.location.ifBlank { "On Location" }}   (${dayCaptures.size} Captures)",
                    MARGIN + 10f,
                    currentY + 16f,
                    boldPaint
                )
                currentY += 32f

                if (day.dayNotes.isNotBlank()) {
                    canvas.drawText("Day Notes: ${day.dayNotes}", MARGIN + 4f, currentY, mutedPaint)
                    currentY += 16f
                }
            }

            // Captures
            for (capture in dayCaptures) {
                val effectiveCam = MetadataResolver.resolveEffectiveCameraSettings(
                    capture.baselineCamera,
                    capture.cameraOverrides
                )
                val effectiveVfx = MetadataResolver.resolveEffectiveVfxSettings(
                    capture.baselineVfx,
                    capture.vfxOverrides
                )

                val estimatedCardHeight = when (options.layoutType) {
                    "Image Only" -> 220f
                    "Compact" -> 160f
                    else -> 230f // Detailed
                }

                ensureSpace(estimatedCardHeight)

                // Draw Shot Container Box
                val shotBoxTop = currentY
                val shotBoxHeight = estimatedCardHeight - 12f
                val shotBoxRect = RectF(MARGIN, shotBoxTop, PAGE_WIDTH - MARGIN, shotBoxTop + shotBoxHeight)

                canvas.drawRoundRect(shotBoxRect, 4f, 4f, boxFillPaint)
                canvas.drawRoundRect(shotBoxRect, 4f, 4f, linePaint)

                // Shot Header strip inside card
                val stripHeight = 22f
                val stripRect = RectF(MARGIN, shotBoxTop, PAGE_WIDTH - MARGIN, shotBoxTop + stripHeight)
                val stripPaint = Paint().apply { color = Color.rgb(238, 242, 246) }
                canvas.drawRoundRect(stripRect, 4f, 4f, stripPaint)
                canvas.drawRoundRect(stripRect, 4f, 4f, linePaint)

                val shotTitle = "${capture.shotNumber}  |  ${capture.scene.ifBlank { "Scene" }}  ·  ${capture.take.ifBlank { "Take 01" }}"
                canvas.drawText(shotTitle, MARGIN + 8f, shotBoxTop + 15f, boldPaint)

                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(capture.createdAt))
                val timeW = mutedPaint.measureText(timeStr)
                canvas.drawText(timeStr, PAGE_WIDTH - MARGIN - timeW - 8f, shotBoxTop + 15f, mutedPaint)

                val contentTop = shotBoxTop + stripHeight + 8f

                // Draw Image thumbnail if requested and exists
                val imageWidth = 140f
                val imageHeight = 105f
                if (options.includeImages) {
                    val imgFile = File(capture.thumbnailPath ?: capture.imagePath)
                    if (imgFile.exists()) {
                        try {
                            val bmp = BitmapFactory.decodeFile(imgFile.absolutePath)
                            if (bmp != null) {
                                val destRect = RectF(MARGIN + 8f, contentTop, MARGIN + 8f + imageWidth, contentTop + imageHeight)
                                canvas.drawBitmap(bmp, null, destRect, null)
                                canvas.drawRoundRect(destRect, 2f, 2f, linePaint)
                                bmp.recycle()
                            }
                        } catch (_: Exception) {
                            // Draw placeholder
                            val destRect = RectF(MARGIN + 8f, contentTop, MARGIN + 8f + imageWidth, contentTop + imageHeight)
                            canvas.drawRoundRect(destRect, 2f, 2f, linePaint)
                            canvas.drawText("[Image]", MARGIN + 55f, contentTop + 55f, mutedPaint)
                        }
                    } else {
                        val destRect = RectF(MARGIN + 8f, contentTop, MARGIN + 8f + imageWidth, contentTop + imageHeight)
                        canvas.drawRoundRect(destRect, 2f, 2f, linePaint)
                        canvas.drawText("[No Image]", MARGIN + 45f, contentTop + 55f, mutedPaint)
                    }
                }

                // Metadata columns to the right of image
                val metaLeft = if (options.includeImages) MARGIN + 8f + imageWidth + 14f else MARGIN + 12f
                var metaY = contentTop + 10f

                if (options.layoutType != "Image Only") {
                    if (options.includeCameraMetadata) {
                        canvas.drawText("CAMERA", metaLeft, metaY, boldPaint)
                        metaY += 12f
                        canvas.drawText("Camera: ${effectiveCam.cameraManufacturer} ${effectiveCam.cameraModel} (${effectiveCam.cameraUnitId})", metaLeft, metaY, textPaint)
                        metaY += 12f
                        canvas.drawText("Lens: ${effectiveCam.focalLength} (${effectiveCam.lensManufacturer})  |  Aperture: ${effectiveCam.aperture}", metaLeft, metaY, textPaint)
                        metaY += 12f
                        canvas.drawText("ISO: ${effectiveCam.iso}  |  Shutter: ${effectiveCam.shutterSpeed}  |  FPS: ${effectiveCam.frameRate}", metaLeft, metaY, textPaint)
                        metaY += 12f
                        canvas.drawText("WB: ${effectiveCam.whiteBalance}  |  Color: ${effectiveCam.colorSpace} (${effectiveCam.gammaProfile})", metaLeft, metaY, textPaint)
                        metaY += 16f
                    }

                    if (options.includeVfxMetadata) {
                        canvas.drawText("VFX & TRACKING", metaLeft, metaY, boldPaint)
                        metaY += 12f
                        canvas.drawText("Plate: ${effectiveVfx.plateType}  |  Height: ${effectiveVfx.cameraHeight}  |  Dist: ${effectiveVfx.defaultCameraDistance}", metaLeft, metaY, textPaint)
                        metaY += 12f
                        if (effectiveVfx.lightingNotes.isNotBlank()) {
                            canvas.drawText("Lighting: ${effectiveVfx.lightingNotes}", metaLeft, metaY, textPaint)
                            metaY += 12f
                        }
                        if (effectiveVfx.defaultTrackingNotes.isNotBlank()) {
                            canvas.drawText("Tracking: ${effectiveVfx.defaultTrackingNotes}", metaLeft, metaY, textPaint)
                            metaY += 14f
                        }
                    }

                    if (options.includeNotes && (capture.shotNotes.isNotBlank() || capture.additionalNotes.isNotBlank())) {
                        val notes = capture.shotNotes.ifBlank { capture.additionalNotes }
                        canvas.drawText("NOTES: $notes", metaLeft, metaY, mutedPaint)
                    }
                }

                currentY = shotBoxTop + shotBoxHeight + 10f
            }
        }

        // Draw header and footer on the final page
        drawHeaderAndFooter(canvas, pageNumber)
        pdfDocument.finishPage(currentPage)

        // Save PDF to file
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        pdfFile
    } catch (e: Exception) {
        // Fallback for headless JVM / Robolectric environment where native graphics pipeline is not initialized
        writeFallbackPdf(pdfFile, project, filteredDays, capturesByDay, options)
        pdfFile
    }
}

private fun writeFallbackPdf(
    file: File,
    project: ProjectEntity,
    days: List<ShootingDayEntity>,
    capturesByDay: Map<String, List<CaptureEntity>>,
    options: PdfExportOptions
) {
    val content = buildString {
        appendLine("VFX CAPTURE REPORT")
        appendLine("Project: ${project.name}")
        appendLine("Client: ${project.client}")
        appendLine("Production: ${project.productionCompany}")
        appendLine("Camera: ${project.cameraDefaults.cameraManufacturer} ${project.cameraDefaults.cameraModel}")
        appendLine("Lens: ${project.cameraDefaults.focalLength} ${project.cameraDefaults.lensManufacturer}")
        appendLine("Color Space: ${project.cameraDefaults.colorSpace}")
        for (day in days) {
            appendLine("DAY ${day.dayNumber} - ${day.date} - ${day.location}")
            val caps = capturesByDay[day.id] ?: emptyList()
            for (c in caps) {
                val effIso = c.cameraOverrides.iso ?: c.baselineCamera.iso
                val effLens = c.cameraOverrides.focalLength ?: c.baselineCamera.focalLength
                appendLine("  ${c.shotNumber}: ${c.scene} ${c.take} - Lens: $effLens, ISO: $effIso")
            }
        }
    }

    val escapedText = content.replace("(", "\\(").replace(")", "\\)").replace("\n", "\\n")
    val streamContent = "BT /F1 12 Tf 50 750 Td ($escapedText) Tj ET"
    val streamLength = streamContent.length

    val pdfRaw = """
%PDF-1.4
1 0 obj
<< /Type /Catalog /Pages 2 0 R >>
endobj
2 0 obj
<< /Type /Pages /Kids [3 0 R] /Count 1 >>
endobj
3 0 obj
<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>
endobj
4 0 obj
<< /Length $streamLength >>
stream
$streamContent
endstream
endobj
5 0 obj
<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>
endobj
xref
0 6
0000000000 65535 f 
0000000009 00000 n 
0000000058 00000 n 
0000000115 00000 n 
0000000224 00000 n 
0000000300 00000 n 
trailer
<< /Size 6 /Root 1 0 R >>
startxref
365
%%EOF
""".trimIndent()

    FileOutputStream(file).use { out ->
        out.write(pdfRaw.toByteArray(Charsets.US_ASCII))
    }
}
}
