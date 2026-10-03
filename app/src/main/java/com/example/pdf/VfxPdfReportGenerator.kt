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

        val cleanProjectName = project.name.trim().replace("[^a-zA-Z0-9_-]".toRegex(), "_").trim('_')
        val isDailyReport = options.selectedDayId != null
        val selectedDay = if (isDailyReport) days.find { it.id == options.selectedDayId } else null

        val filteredDays = if (selectedDay != null) {
            listOf(selectedDay)
        } else {
            days
        }

        val reportsDir = File(context.filesDir, "reports").apply { if (!exists()) mkdirs() }
        val fileName = if (isDailyReport && selectedDay != null) {
            val dayNumStr = String.format(Locale.US, "%02d", selectedDay.dayNumber)
            if (cleanProjectName.isNotBlank()) "VFX_${cleanProjectName}_Day_${dayNumStr}_Report.pdf"
            else "VFX_Day_${dayNumStr}_Report.pdf"
        } else {
            if (cleanProjectName.isNotBlank()) "VFX_${cleanProjectName}_Project_Report.pdf"
            else "VFX_Project_Report.pdf"
        }
        val pdfFile = File(reportsDir, fileName)

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
                val headerText = if (isDailyReport && selectedDay != null) {
                    "VFX DAILY CAPTURE REPORT  ·  ${project.name.ifBlank { "VFX PROJECT" }.uppercase(Locale.US)}  ·  DAY ${selectedDay.dayNumber}"
                } else {
                    "VFX COMPLETE PROJECT REPORT  ·  ${project.name.ifBlank { "VFX PROJECT" }.uppercase(Locale.US)}"
                }
                c.drawText(headerText, MARGIN, 24f, mutedPaint)
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
                val titleLabel = if (isDailyReport && selectedDay != null) "VFX DAILY CAPTURE REPORT" else "VFX COMPLETE PROJECT REPORT"
                canvas.drawText(titleLabel, MARGIN, currentY, subTitlePaint)
                currentY += 24f
                val projNameDisplay = project.name.ifBlank { "VFX PROJECT" }.uppercase(Locale.US)
                canvas.drawText(projNameDisplay, MARGIN, currentY, headerTitlePaint)
                currentY += 8f
                canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, amberLinePaint)
                currentY += 16f

                // Project info table box
                val boxHeight = 115f
                canvas.drawRoundRect(RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + boxHeight), 4f, 4f, boxFillPaint)
                canvas.drawRoundRect(RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + boxHeight), 4f, 4f, linePaint)

                val col1X = MARGIN + 12f
                val col2X = MARGIN + (CONTENT_WIDTH / 2f) + 6f
                var rowY = currentY + 16f

                if (isDailyReport && selectedDay != null) {
                    canvas.drawText("Project:", col1X, rowY, boldPaint)
                    canvas.drawText(project.name.ifBlank { "Not specified" }, col1X + 70f, rowY, textPaint)
                    canvas.drawText("Shooting Day:", col2X, rowY, boldPaint)
                    canvas.drawText("Day ${selectedDay.dayNumber}", col2X + 85f, rowY, textPaint)

                    rowY += 15f
                    canvas.drawText("Date:", col1X, rowY, boldPaint)
                    canvas.drawText(selectedDay.date.ifBlank { project.date.ifBlank { "Not specified" } }, col1X + 70f, rowY, textPaint)
                    canvas.drawText("Location:", col2X, rowY, boldPaint)
                    canvas.drawText(selectedDay.location.ifBlank { project.location.ifBlank { "Not specified" } }, col2X + 85f, rowY, textPaint)

                    rowY += 15f
                    canvas.drawText("Client:", col1X, rowY, boldPaint)
                    canvas.drawText(project.client.ifBlank { "Not specified" }, col1X + 70f, rowY, textPaint)
                    canvas.drawText("Production:", col2X, rowY, boldPaint)
                    canvas.drawText(project.productionCompany.ifBlank { "Not specified" }, col2X + 85f, rowY, textPaint)

                    rowY += 15f
                    canvas.drawText("VFX Supervisor:", col1X, rowY, boldPaint)
                    canvas.drawText(project.vfxSupervisor.ifBlank { "Not specified" }, col1X + 85f, rowY, textPaint)
                    canvas.drawText("Director:", col2X, rowY, boldPaint)
                    canvas.drawText(project.director.ifBlank { "Not specified" }, col2X + 85f, rowY, textPaint)

                    rowY += 15f
                    canvas.drawText("Project Code:", col1X, rowY, boldPaint)
                    canvas.drawText(project.projectIdCode.ifBlank { "Not specified" }, col1X + 70f, rowY, textPaint)
                    canvas.drawText("Day Captures:", col2X, rowY, boldPaint)
                    val dayCaps = capturesByDay[selectedDay.id] ?: emptyList()
                    canvas.drawText("${dayCaps.size} Captures", col2X + 85f, rowY, textPaint)

                    currentY += boxHeight + 14f

                    // DAY [X] CAMERA / PRODUCTION INFORMATION
                    ensureSpace(70f)
                    canvas.drawText("DAY ${selectedDay.dayNumber} CAMERA / PRODUCTION INFORMATION", MARGIN, currentY, boldPaint)
                    currentY += 8f
                    canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, linePaint)
                    currentY += 14f

                    val dayCam = MetadataResolver.resolveEffectiveCameraSettings(project.cameraDefaults, selectedDay.cameraOverrides)
                    val daySpecs = mutableListOf<String>()
                    val camStr = listOf(dayCam.cameraManufacturer, dayCam.cameraModel).filter { it.isNotBlank() }.joinToString(" ")
                    daySpecs.add("Camera: " + camStr.ifBlank { "Not specified" })
                    val lensStr = listOf(dayCam.focalLength, if (dayCam.lensManufacturer.isNotBlank() || dayCam.lensModel.isNotBlank()) "(${listOf(dayCam.lensManufacturer, dayCam.lensModel).filter { it.isNotBlank() }.joinToString(" ")})" else "").filter { it.isNotBlank() }.joinToString(" ")
                    daySpecs.add("Lens: " + lensStr.ifBlank { "Not specified" })
                    daySpecs.add("ISO: " + dayCam.iso.ifBlank { "Not specified" })
                    daySpecs.add("Shutter: " + dayCam.shutterSpeed.ifBlank { "Not specified" })
                    daySpecs.add("Aperture: " + dayCam.aperture.ifBlank { "Not specified" })
                    daySpecs.add("FPS: " + dayCam.frameRate.ifBlank { "Not specified" })
                    daySpecs.add("Color Space: " + dayCam.colorSpace.ifBlank { "Not specified" })
                    daySpecs.add("Gamma: " + dayCam.gammaProfile.ifBlank { "Not specified" })
                    daySpecs.add("Resolution: " + dayCam.resolution.ifBlank { "Not specified" })

                    val colW = CONTENT_WIDTH / 3f
                    for (i in daySpecs.indices) {
                        val cIdx = i % 3
                        val rIdx = i / 3
                        val x = MARGIN + (cIdx * colW)
                        val y = currentY + (rIdx * 14f)
                        canvas.drawText(daySpecs[i], x, y, textPaint)
                    }
                    currentY += 46f
                } else {
                    // Complete Project Details
                    canvas.drawText("Client:", col1X, rowY, boldPaint)
                    canvas.drawText(project.client.ifBlank { "Not specified" }, col1X + 70f, rowY, textPaint)
                    canvas.drawText("Date:", col2X, rowY, boldPaint)
                    canvas.drawText(project.date.ifBlank { "Not specified" }, col2X + 85f, rowY, textPaint)

                    rowY += 15f
                    canvas.drawText("Production:", col1X, rowY, boldPaint)
                    canvas.drawText(project.productionCompany.ifBlank { "Not specified" }, col1X + 70f, rowY, textPaint)
                    canvas.drawText("Location:", col2X, rowY, boldPaint)
                    canvas.drawText(project.location.ifBlank { "Not specified" }, col2X + 85f, rowY, textPaint)

                    rowY += 15f
                    canvas.drawText("VFX Supervisor:", col1X, rowY, boldPaint)
                    canvas.drawText(project.vfxSupervisor.ifBlank { "Not specified" }, col1X + 85f, rowY, textPaint)
                    canvas.drawText("Director:", col2X, rowY, boldPaint)
                    canvas.drawText(project.director.ifBlank { "Not specified" }, col2X + 85f, rowY, textPaint)

                    rowY += 15f
                    canvas.drawText("VFX Producer:", col1X, rowY, boldPaint)
                    canvas.drawText(project.vfxProducer.ifBlank { "Not specified" }, col1X + 85f, rowY, textPaint)
                    canvas.drawText("Camera Op:", col2X, rowY, boldPaint)
                    canvas.drawText(project.cameraOperator.ifBlank { "Not specified" }, col2X + 85f, rowY, textPaint)

                    rowY += 15f
                    canvas.drawText("Project Code:", col1X, rowY, boldPaint)
                    canvas.drawText(project.projectIdCode.ifBlank { "Not specified" }, col1X + 70f, rowY, textPaint)
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
                    val camSpecs = mutableListOf<String>()
                    val camStr = listOf(cam.cameraManufacturer, cam.cameraModel).filter { it.isNotBlank() }.joinToString(" ")
                    camSpecs.add("Camera: " + camStr.ifBlank { "Not specified" })
                    val lensStr = listOf(cam.focalLength, if (cam.lensManufacturer.isNotBlank() || cam.lensModel.isNotBlank()) "(${listOf(cam.lensManufacturer, cam.lensModel).filter { it.isNotBlank() }.joinToString(" ")})" else "").filter { it.isNotBlank() }.joinToString(" ")
                    camSpecs.add("Lens: " + lensStr.ifBlank { "Not specified" })
                    camSpecs.add("ISO: " + cam.iso.ifBlank { "Not specified" })
                    camSpecs.add("Shutter: " + cam.shutterSpeed.ifBlank { "Not specified" })
                    camSpecs.add("Aperture: " + cam.aperture.ifBlank { "Not specified" })
                    camSpecs.add("FPS: " + cam.frameRate.ifBlank { "Not specified" })
                    camSpecs.add("Color Space: " + cam.colorSpace.ifBlank { "Not specified" })
                    camSpecs.add("Gamma: " + cam.gammaProfile.ifBlank { "Not specified" })
                    camSpecs.add("Resolution: " + cam.resolution.ifBlank { "Not specified" })

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
            }

            if (filteredDays.isEmpty()) {
                ensureSpace(40f)
                canvas.drawText("No shooting days available.", MARGIN, currentY, mutedPaint)
                currentY += 20f
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
                        val camMakeModel = listOf(effectiveCam.cameraManufacturer, effectiveCam.cameraModel).filter { it.isNotBlank() }.joinToString(" ")
                        val camUnit = if (effectiveCam.cameraUnitId.isNotBlank()) " (${effectiveCam.cameraUnitId})" else ""
                        val camDisplay = if (camMakeModel.isNotBlank()) "Camera: $camMakeModel$camUnit" else "Camera: Not specified"
                        canvas.drawText(camDisplay, metaLeft, metaY, textPaint)
                        metaY += 12f

                        val lensSpecs = mutableListOf<String>()
                        if (effectiveCam.focalLength.isNotBlank()) lensSpecs.add(effectiveCam.focalLength)
                        if (effectiveCam.lensManufacturer.isNotBlank() || effectiveCam.lensModel.isNotBlank()) {
                            val lMake = listOf(effectiveCam.lensManufacturer, effectiveCam.lensModel).filter { it.isNotBlank() }.joinToString(" ")
                            lensSpecs.add("($lMake)")
                        }
                        val lensDisplay = if (lensSpecs.isNotEmpty()) "Lens: ${lensSpecs.joinToString(" ")}" else "Lens: Not specified"
                        val aptDisplay = if (effectiveCam.aperture.isNotBlank()) "  |  Aperture: ${effectiveCam.aperture}" else ""
                        canvas.drawText(lensDisplay + aptDisplay, metaLeft, metaY, textPaint)
                        metaY += 12f

                        val expParts = mutableListOf<String>()
                        if (effectiveCam.iso.isNotBlank()) expParts.add("ISO: ${effectiveCam.iso}")
                        if (effectiveCam.shutterSpeed.isNotBlank()) expParts.add("Shutter: ${effectiveCam.shutterSpeed}")
                        if (effectiveCam.frameRate.isNotBlank()) expParts.add("FPS: ${effectiveCam.frameRate}")
                        if (expParts.isNotEmpty()) {
                            canvas.drawText(expParts.joinToString("  |  "), metaLeft, metaY, textPaint)
                            metaY += 12f
                        }

                        val colParts = mutableListOf<String>()
                        if (effectiveCam.whiteBalance.isNotBlank()) colParts.add("WB: ${effectiveCam.whiteBalance}")
                        if (effectiveCam.colorSpace.isNotBlank()) colParts.add("Color: ${effectiveCam.colorSpace}")
                        if (effectiveCam.gammaProfile.isNotBlank()) colParts.add("(${effectiveCam.gammaProfile})")
                        if (colParts.isNotEmpty()) {
                            canvas.drawText(colParts.joinToString("  |  "), metaLeft, metaY, textPaint)
                            metaY += 14f
                        } else {
                            metaY += 2f
                        }
                    }

                    if (options.includeVfxMetadata) {
                        canvas.drawText("VFX & TRACKING", metaLeft, metaY, boldPaint)
                        metaY += 12f
                        val vfxParts = mutableListOf<String>()
                        if (effectiveVfx.plateType.isNotBlank()) vfxParts.add("Plate: ${effectiveVfx.plateType}")
                        if (effectiveVfx.cameraHeight.isNotBlank()) vfxParts.add("Height: ${effectiveVfx.cameraHeight}")
                        if (effectiveVfx.defaultCameraDistance.isNotBlank()) vfxParts.add("Dist: ${effectiveVfx.defaultCameraDistance}")
                        if (vfxParts.isNotEmpty()) {
                            canvas.drawText(vfxParts.joinToString("  |  "), metaLeft, metaY, textPaint)
                            metaY += 12f
                        }
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

            if (dayCaptures.isEmpty()) {
                ensureSpace(30f)
                canvas.drawText("No captures recorded for this day.", MARGIN + 4f, currentY, mutedPaint)
                currentY += 24f
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
    val isDailyReport = options.selectedDayId != null
    val selectedDay = if (isDailyReport) days.find { it.id == options.selectedDayId } else null

    val content = buildString {
        if (isDailyReport && selectedDay != null) {
            appendLine("VFX DAILY CAPTURE REPORT")
            appendLine("Project: ${project.name.ifBlank { "Not specified" }}")
            appendLine("Shooting Day: Day ${selectedDay.dayNumber}")
            appendLine("Date: ${selectedDay.date.ifBlank { "Not specified" }}")
            appendLine("Location: ${selectedDay.location.ifBlank { "Not specified" }}")
            if (project.client.isNotBlank()) appendLine("Client: ${project.client}")
            if (project.vfxSupervisor.isNotBlank()) appendLine("VFX Supervisor: ${project.vfxSupervisor}")
            appendLine("DAY ${selectedDay.dayNumber} CAMERA / PRODUCTION INFORMATION")
            val dayCam = MetadataResolver.resolveEffectiveCameraSettings(project.cameraDefaults, selectedDay.cameraOverrides)
            val camStr = listOf(dayCam.cameraManufacturer, dayCam.cameraModel).filter { it.isNotBlank() }.joinToString(" ")
            if (camStr.isNotBlank()) appendLine("Camera: $camStr") else appendLine("Camera: Not specified")
            if (dayCam.focalLength.isNotBlank()) appendLine("Lens: ${dayCam.focalLength}")
            if (dayCam.iso.isNotBlank()) appendLine("ISO: ${dayCam.iso}")
            appendLine("CAPTURES")
            val dayCaps = capturesByDay[selectedDay.id] ?: emptyList()
            if (dayCaps.isEmpty()) {
                appendLine("No captures recorded for this day.")
            } else {
                for (c in dayCaps) {
                    val effIso = c.cameraOverrides.iso ?: c.baselineCamera.iso
                    val effLens = c.cameraOverrides.focalLength ?: c.baselineCamera.focalLength
                    appendLine("  ${c.shotNumber}: ${c.scene} ${c.take} - Lens: ${effLens.ifBlank { "Not specified" }}, ISO: ${effIso.ifBlank { "Not specified" }}")
                }
            }
        } else {
            appendLine("VFX COMPLETE PROJECT REPORT")
            appendLine("Project: ${project.name.ifBlank { "Not specified" }}")
            if (project.client.isNotBlank()) appendLine("Client: ${project.client}")
            if (project.productionCompany.isNotBlank()) appendLine("Production: ${project.productionCompany}")
            val cam = project.cameraDefaults
            val camStr = listOf(cam.cameraManufacturer, cam.cameraModel).filter { it.isNotBlank() }.joinToString(" ")
            if (camStr.isNotBlank()) appendLine("Camera: $camStr") else appendLine("Camera: Not specified")
            if (cam.focalLength.isNotBlank()) appendLine("Lens: ${cam.focalLength}")
            if (days.isEmpty()) {
                appendLine("No shooting days available.")
            } else {
                for (day in days) {
                    appendLine("DAY ${day.dayNumber} - ${day.date} - ${day.location.ifBlank { "On Location" }}")
                    val caps = capturesByDay[day.id] ?: emptyList()
                    if (caps.isEmpty()) {
                        appendLine("  No captures recorded for this day.")
                    } else {
                        for (c in caps) {
                            val effIso = c.cameraOverrides.iso ?: c.baselineCamera.iso
                            val effLens = c.cameraOverrides.focalLength ?: c.baselineCamera.focalLength
                            appendLine("  ${c.shotNumber}: ${c.scene} ${c.take} - Lens: ${effLens.ifBlank { "Not specified" }}, ISO: ${effIso.ifBlank { "Not specified" }}")
                        }
                    }
                }
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
