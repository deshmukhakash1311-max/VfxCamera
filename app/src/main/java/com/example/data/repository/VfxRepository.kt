package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.withTransaction
import com.example.data.demo.DemoProject
import com.example.data.local.CaptureDao
import com.example.data.local.ExportedReportDao
import com.example.data.local.ProjectDao
import com.example.data.local.ShootingDayDao
import com.example.data.local.VfxDatabase
import com.example.data.model.CameraOverrides
import com.example.data.model.CaptureEntity
import com.example.data.model.ExportedReportEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ShootingDayEntity
import com.example.data.model.VfxOverrides
import com.example.domain.inheritance.MetadataResolver
import com.example.pdf.PdfExportOptions
import com.example.pdf.VfxPdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class VfxRepository(
    private val context: Context,
    private val database: VfxDatabase = VfxDatabase.getDatabase(context)
) {
    private val projectDao: ProjectDao = database.projectDao()
    private val shootingDayDao: ShootingDayDao = database.shootingDayDao()
    private val captureDao: CaptureDao = database.captureDao()
    private val exportedReportDao: ExportedReportDao = database.exportedReportDao()

    val activeProjects: Flow<List<ProjectEntity>> = projectDao.getActiveProjects()
    val archivedProjects: Flow<List<ProjectEntity>> = projectDao.getArchivedProjects()
    val allReports: Flow<List<ExportedReportEntity>> = exportedReportDao.getAllReports()

    suspend fun getProject(id: String): ProjectEntity? = withContext(Dispatchers.IO) {
        projectDao.getProjectById(id)
    }

    fun observeProject(id: String): Flow<ProjectEntity?> = projectDao.observeProjectById(id)

    suspend fun saveProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        projectDao.insertProject(updated)
    }

    suspend fun deleteProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        if (DemoProject.isDemo(project)) cleanUpDemoFiles(project.id)
        projectDao.deleteProject(project)
    }

    suspend fun setProjectArchived(id: String, archived: Boolean) = withContext(Dispatchers.IO) {
        projectDao.setArchived(id, archived)
    }

    // Shooting Day operations
    fun getShootingDays(projectId: String): Flow<List<ShootingDayEntity>> =
        shootingDayDao.getShootingDaysForProject(projectId)

    suspend fun getShootingDaysList(projectId: String): List<ShootingDayEntity> = withContext(Dispatchers.IO) {
        shootingDayDao.getShootingDaysListForProject(projectId)
    }

    suspend fun getShootingDay(id: String): ShootingDayEntity? = withContext(Dispatchers.IO) {
        shootingDayDao.getShootingDayById(id)
    }

    fun observeShootingDay(id: String): Flow<ShootingDayEntity?> =
        shootingDayDao.observeShootingDayById(id)

    suspend fun createShootingDay(
        projectId: String,
        date: String = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date()),
        location: String = "",
        dayNotes: String = "",
        cameraOverrides: CameraOverrides = CameraOverrides(),
        vfxOverrides: VfxOverrides = VfxOverrides()
    ): ShootingDayEntity = withContext(Dispatchers.IO) {
        val maxDay = shootingDayDao.getMaxDayNumber(projectId) ?: 0
        val nextDayNum = maxDay + 1
        val newDay = ShootingDayEntity(
            projectId = projectId,
            dayNumber = nextDayNum,
            date = date,
            location = location,
            dayNotes = dayNotes,
            cameraOverrides = cameraOverrides,
            vfxOverrides = vfxOverrides
        )
        shootingDayDao.insertShootingDay(newDay)
        newDay
    }

    suspend fun saveShootingDay(day: ShootingDayEntity) = withContext(Dispatchers.IO) {
        val updated = day.copy(updatedAt = System.currentTimeMillis())
        shootingDayDao.updateShootingDay(updated)
    }

    suspend fun deleteShootingDay(day: ShootingDayEntity) = withContext(Dispatchers.IO) {
        shootingDayDao.deleteShootingDay(day)
    }

    // Capture operations
    fun getCapturesForDay(dayId: String): Flow<List<CaptureEntity>> =
        captureDao.getCapturesForDay(dayId)

    suspend fun getCapturesListForDay(dayId: String): List<CaptureEntity> = withContext(Dispatchers.IO) {
        captureDao.getCapturesListForDay(dayId)
    }

    fun getCapturesForProject(projectId: String): Flow<List<CaptureEntity>> =
        captureDao.getCapturesForProject(projectId)

    suspend fun getCapturesListForProject(projectId: String): List<CaptureEntity> = withContext(Dispatchers.IO) {
        captureDao.getCapturesListForProject(projectId)
    }

    suspend fun getCapture(id: String): CaptureEntity? = withContext(Dispatchers.IO) {
        captureDao.getCaptureById(id)
    }

    fun observeCapture(id: String): Flow<CaptureEntity?> = captureDao.observeCaptureById(id)

    /**
     * Create and record a new Capture:
     * - Saves the image to private local storage
     * - Creates and freezes baseline metadata at capture moment (Principle 42)
     * - Records real hardware readings if available
     * - Returns the new CaptureEntity
     */
    suspend fun recordCapture(
        projectId: String,
        dayId: String,
        imageBytes: ByteArray,
        scene: String = "",
        take: String = "Take 01",
        subject: String = "",
        description: String = "",
        originalCameraIso: String? = null,
        originalCameraShutter: String? = null,
        originalCameraAperture: String? = null,
        originalCameraFocalLength: String? = null,
        originalCameraWhiteBalance: String? = null,
        originalCameraResolution: String? = null,
        originalCameraExposureComp: String? = null,
        originalCameraLensModel: String? = null,
        shotCameraOverrides: CameraOverrides = CameraOverrides(),
        shotVfxOverrides: VfxOverrides = VfxOverrides(),
        shotNotes: String = "",
        additionalNotes: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        altitude: Double? = null,
        locationName: String? = null
    ): CaptureEntity = withContext(Dispatchers.IO) {
        val project = projectDao.getProjectById(projectId) ?: throw IllegalStateException("Project not found: $projectId")
        val day = shootingDayDao.getShootingDayById(dayId) ?: throw IllegalStateException("Day not found: $dayId")

        // 1. Save image to local disk
        val captureId = UUID.randomUUID().toString()
        val imagesDir = File(context.filesDir, "captures").apply { if (!exists()) mkdirs() }
        val imageFile = File(imagesDir, "vfx_shot_${captureId}.jpg")
        FileOutputStream(imageFile).use { it.write(imageBytes) }

        // 2. Generate and save thumbnail
        val thumbFile = File(imagesDir, "vfx_thumb_${captureId}.jpg")
        try {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap != null) {
                val thumbSize = 256
                val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                val (targetW, targetH) = if (aspect >= 1f) {
                    Pair(thumbSize, (thumbSize / aspect).toInt())
                } else {
                    Pair((thumbSize * aspect).toInt(), thumbSize)
                }
                val thumb = Bitmap.createScaledBitmap(bitmap, targetW.coerceAtLeast(1), targetH.coerceAtLeast(1), true)
                FileOutputStream(thumbFile).use { out ->
                    thumb.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                if (thumb != bitmap) thumb.recycle()
                bitmap.recycle()
            }
        } catch (_: Exception) {
            // Fallback to full image path if thumbnail creation fails
        }

        // 3. Freeze baseline metadata from current project + day overrides (Principle 42)
        val (baselineCam, baselineVfx) = MetadataResolver.buildBaselineForNewCapture(project, day)

        // 4. Calculate sequence number
        val maxSeq = captureDao.getMaxSequenceNumberForDay(dayId) ?: 0
        val sequenceNumber = maxSeq + 1
        val shotNumber = MetadataResolver.formatShotNumber(sequenceNumber)

        val capture = CaptureEntity(
            id = captureId,
            projectId = projectId,
            shootingDayId = dayId,
            shotNumber = shotNumber,
            shotSequenceNumber = sequenceNumber,
            scene = scene,
            take = take,
            subject = subject,
            description = description,
            imagePath = imageFile.absolutePath,
            thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else imageFile.absolutePath,
            originalCameraIso = originalCameraIso,
            originalCameraShutter = originalCameraShutter,
            originalCameraAperture = originalCameraAperture,
            originalCameraFocalLength = originalCameraFocalLength,
            originalCameraWhiteBalance = originalCameraWhiteBalance,
            originalCameraResolution = originalCameraResolution,
            originalCameraExposureComp = originalCameraExposureComp,
            originalCameraLensModel = originalCameraLensModel,
            baselineCamera = baselineCam,
            baselineVfx = baselineVfx,
            cameraOverrides = shotCameraOverrides,
            vfxOverrides = shotVfxOverrides,
            shotNotes = shotNotes,
            additionalNotes = additionalNotes,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            locationName = locationName
        )

        captureDao.insertCapture(capture)
        projectDao.setArchived(projectId, project.isArchived, System.currentTimeMillis())
        capture
    }

    suspend fun saveCapture(capture: CaptureEntity) = withContext(Dispatchers.IO) {
        val updated = capture.copy(updatedAt = System.currentTimeMillis())
        captureDao.updateCapture(updated)
    }

    suspend fun deleteCapture(capture: CaptureEntity) = withContext(Dispatchers.IO) {
        captureDao.deleteCapture(capture)
    }

    suspend fun setReference(captureId: String, isReference: Boolean) = withContext(Dispatchers.IO) {
        captureDao.setReferenceStatus(captureId, isReference)
    }

    // Reports
    suspend fun saveReport(report: ExportedReportEntity) = withContext(Dispatchers.IO) {
        exportedReportDao.insertReport(report)
    }

    suspend fun deleteReport(report: ExportedReportEntity) = withContext(Dispatchers.IO) {
        val file = File(report.filePath)
        if (file.exists()) file.delete()
        exportedReportDao.deleteReport(report)
    }

    /**
     * Explicitly loads the sample "Project Aurora" demo project (3 days x 3 captures, 9 bundled
     * demo images and the four demo PDF reports). Only ever called from a user action, never
     * automatically on a fresh install. If the demo is already loaded it is returned unchanged
     * instead of creating a duplicate; delete it like any other project to load it again.
     */
    suspend fun loadDemoProject(): DemoLoadResult = demoMutex.withLock {
        withContext(Dispatchers.IO) {
            projectDao.getProjectByCode(DemoProject.CODE)?.let {
                return@withContext DemoLoadResult(it, alreadyLoaded = true)
            }

            val demoDir = File(context.filesDir, DemoProject.ASSET_DIR).apply { if (!exists()) mkdirs() }
            val key = UUID.randomUUID().toString().take(8)
            fun imageFile(day: Int, shot: Int) = File(demoDir, "aurora_${key}_${DemoProject.assetName(day, shot)}")

            val spec = DemoProject.build { day, shot -> imageFile(day, shot).absolutePath }
            val copied = mutableListOf<File>()
            try {
                for (d in spec.days) for (s in d.shots) {
                    val target = File(s.shot.imagePath)
                    context.assets.open("${DemoProject.ASSET_DIR}/${s.assetName}").use { input ->
                        FileOutputStream(target).use { out -> input.copyTo(out) }
                    }
                    copied.add(target)
                }
                database.withTransaction {
                    projectDao.insertProject(spec.project)
                    for (d in spec.days) {
                        shootingDayDao.insertShootingDay(d.day)
                        for (s in d.shots) captureDao.insertCapture(s.shot)
                    }
                }
            } catch (e: Exception) {
                copied.forEach { it.delete() }
                throw e
            }

            // Demo PDFs are best-effort: a report failure must not undo a successfully loaded demo.
            try {
                generateDemoReports(spec)
            } catch (_: Exception) {
            }
            DemoLoadResult(spec.project, alreadyLoaded = false)
        }
    }

    private suspend fun generateDemoReports(spec: DemoProject.Spec) {
        val days = spec.days.map { it.day }
        val capturesByDay = spec.days.associate { it.day.id to it.shots.map { s -> s.shot } }
        val layout = "Detailed"

        suspend fun export(selectedDayId: String?, label: String, shotCount: Int) {
            val generated = VfxPdfReportGenerator.generateReportWithInfo(
                context = context,
                project = spec.project,
                days = days,
                capturesByDay = capturesByDay,
                options = PdfExportOptions(layoutType = layout, selectedDayId = selectedDayId)
            )
            val file = generated.file
            exportedReportDao.insertReport(
                ExportedReportEntity(
                    projectId = spec.project.id,
                    projectName = spec.project.name,
                    fileName = file.name,
                    filePath = file.absolutePath,
                    fileSize = file.length(),
                    pageCount = generated.pageCount,
                    shotCount = shotCount,
                    layoutType = "$label - $layout"
                )
            )
        }

        for (d in days) {
            export(d.id, "Daily Report (Day ${d.dayNumber})", capturesByDay[d.id]?.size ?: 0)
        }
        export(null, "Complete Project Report", capturesByDay.values.sumOf { it.size })
    }

    /** Removes the demo's exported PDFs and copied image files; used when the demo is deleted. */
    private suspend fun cleanUpDemoFiles(projectId: String) {
        exportedReportDao.getReportsListForProject(projectId).forEach { report ->
            File(report.filePath).delete()
            exportedReportDao.deleteReport(report)
        }
        captureDao.getCapturesListForProject(projectId).forEach { capture ->
            File(capture.imagePath).delete()
        }
    }

    companion object {
        private val demoMutex = Mutex()
    }
}

data class DemoLoadResult(val project: ProjectEntity, val alreadyLoaded: Boolean)
