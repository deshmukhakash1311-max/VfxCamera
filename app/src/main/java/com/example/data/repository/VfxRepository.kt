package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.data.local.CaptureDao
import com.example.data.local.ExportedReportDao
import com.example.data.local.ProjectDao
import com.example.data.local.ShootingDayDao
import com.example.data.local.VfxDatabase
import com.example.data.model.CameraOverrides
import com.example.data.model.CameraSettings
import com.example.data.model.CaptureEntity
import com.example.data.model.ExportedReportEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ShootingDayEntity
import com.example.data.model.VfxOverrides
import com.example.data.model.VfxSettings
import com.example.domain.inheritance.MetadataResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
     * Explicitly loads a sample demo project "[DEMO] Project Falcon" only
     * when the user deliberately requests it. Never run automatically on new installs.
     */
    suspend fun loadDemoProject(): ProjectEntity = withContext(Dispatchers.IO) {
        val falconProject = ProjectEntity(
            name = "[DEMO] Project Falcon",
            client = "XYZ Studios (Demo)",
            productionCompany = "Falcon Films",
            productionShow = "Falcon: Dawn of the Cyber Sentinel",
            projectIdCode = "DEMO-2026",
            description = "Demo VFX plate photography, tracking pass and screen replacements.",
            director = "Sarah Connor",
            vfxSupervisor = "Marcus Vance",
            vfxProducer = "Elena Rostova",
            cameraOperator = "Dave K.",
            date = "03 Oct 2026",
            location = "Stage 4, Pinewood & Pune Exterior",
            cameraDefaults = CameraSettings(
                cameraManufacturer = "Sony",
                cameraModel = "FX6",
                cameraUnitId = "A-CAM",
                lensManufacturer = "Zeiss",
                lensModel = "Supreme Prime",
                lensId = "SP-24",
                focalLength = "24mm",
                sensorFormat = "Full Frame 35mm",
                sensorSize = "35.7 x 18.8 mm",
                resolution = "4096x2160 (4K DCI)",
                frameRate = "24 fps",
                iso = "800",
                shutterSpeed = "1/48",
                aperture = "f/2.8",
                whiteBalance = "5600K",
                exposureCompensation = "0.0 EV",
                colorSpace = "S-Gamut3.Cine",
                gammaProfile = "S-Log3",
                recordingFormat = "XAVC-I 422 10-bit"
            ),
            vfxDefaults = VfxSettings(
                plateType = "Reference / Monitor Pass",
                environment = "Interior Stage with Green Tracking Marks",
                lightingNotes = "5600K Key light, 4000K monitor spill",
                generalVfxNotes = "Avoid reflections on control room terminals",
                cameraHeight = "1.5 m",
                defaultCameraDistance = "2.8 m",
                defaultTrackingNotes = "Orange X markers placed 30cm apart on screen perimeter"
            )
        )
        projectDao.insertProject(falconProject)

        // Create Day 1
        val day1 = ShootingDayEntity(
            projectId = falconProject.id,
            dayNumber = 1,
            date = "03 Oct 2026",
            location = "Pune, Stage 4",
            dayNotes = "Control room hero scene setups. Monitor screen replacement passes."
        )
        shootingDayDao.insertShootingDay(day1)

        falconProject
    }
}
