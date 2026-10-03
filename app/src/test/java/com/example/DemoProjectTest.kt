package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.demo.DemoProject
import com.example.data.local.VfxDatabase
import com.example.data.model.CameraSettings
import com.example.data.model.CaptureEntity
import com.example.data.model.InheritanceSource
import com.example.data.model.ProjectEntity
import com.example.data.model.ShootingDayEntity
import com.example.data.model.VfxSettings
import com.example.data.repository.VfxRepository
import com.example.domain.inheritance.MetadataResolver
import com.example.pdf.PdfExportOptions
import com.example.pdf.VfxPdfReportGenerator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DemoProjectTest {

    private lateinit var db: VfxDatabase
    private lateinit var context: Context
    private lateinit var repository: VfxRepository

    private val sceneNames = mapOf(
        1 to listOf("Stage Establishing", "Green Screen Plate", "Clean Plate"),
        2 to listOf("Marker Wall", "Foreground Prop", "Lighting Rig"),
        3 to listOf("Skyline", "Chrome Ball", "Sun Position")
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, VfxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = VfxRepository(context, db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun loadDemo(): ProjectEntity = repository.loadDemoProject().project

    private suspend fun days(projectId: String) = db.shootingDayDao().getShootingDaysListForProject(projectId)

    private suspend fun capturesOf(day: ShootingDayEntity) = db.captureDao().getCapturesListForDay(day.id)

    private suspend fun dailyReport(project: ProjectEntity, dayNumber: Int, layout: String = "Detailed"): Pair<File, String> {
        val allDays = days(project.id)
        val day = allDays.first { it.dayNumber == dayNumber }
        val file = VfxPdfReportGenerator.generateReport(
            context = context,
            project = project,
            days = allDays,
            capturesByDay = allDays.associate { it.id to capturesOf(it) },
            options = PdfExportOptions(layoutType = layout, selectedDayId = day.id)
        )
        return file to file.readText(Charsets.ISO_8859_1)
    }

    private suspend fun projectReport(project: ProjectEntity, layout: String = "Detailed"): Pair<File, String> {
        val allDays = days(project.id)
        val file = VfxPdfReportGenerator.generateReport(
            context = context,
            project = project,
            days = allDays,
            capturesByDay = allDays.associate { it.id to capturesOf(it) },
            options = PdfExportOptions(layoutType = layout)
        )
        return file to file.readText(Charsets.ISO_8859_1)
    }

    private fun scenesOtherThan(day: Int) = sceneNames.filterKeys { it != day }.values.flatten()

    private fun countOf(text: String, needle: String) = text.windowed(needle.length).count { it == needle }

    // 1
    @Test
    fun `fresh database contains zero projects and no demo data`() = runBlocking {
        assertEquals(0, db.projectDao().getProjectCount())
        assertEquals(null, db.projectDao().getProjectByCode(DemoProject.CODE))
    }

    // 2
    @Test
    fun `loading demo explicitly creates exactly one demo project`() = runBlocking {
        val project = loadDemo()
        assertEquals(1, db.projectDao().getProjectCount())
        assertEquals("Project Aurora", project.name)
        assertTrue(DemoProject.isDemo(project))
        assertEquals("Aurora Studios", project.client)
        assertEquals("Demo Director", project.director)
        assertEquals("Demo VFX Supervisor", project.vfxSupervisor)
        assertEquals("Demo Camera Operator", project.cameraOperator)
        assertEquals("Sony", project.cameraDefaults.cameraManufacturer)
        assertEquals("FX6", project.cameraDefaults.cameraModel)
        assertEquals("35mm", project.cameraDefaults.focalLength)
        assertEquals("3840×2160", project.cameraDefaults.resolution)
    }

    // 3, 4, 5
    @Test
    fun `demo has 3 days with 3 captures each and 9 in total`() = runBlocking {
        val project = loadDemo()
        val allDays = days(project.id)
        assertEquals(3, allDays.size)
        assertEquals(listOf(1, 2, 3), allDays.map { it.dayNumber })
        for (day in allDays) assertEquals(3, capturesOf(day).size)
        assertEquals(9, db.captureDao().getCaptureCountForProject(project.id))
    }

    // 6
    @Test
    fun `every capture has a valid local image`() = runBlocking {
        val project = loadDemo()
        for (capture in db.captureDao().getCapturesListForProject(project.id)) {
            val file = File(capture.imagePath)
            assertTrue("Missing image ${capture.imagePath}", file.isFile && file.length() > 0)
            val header = file.inputStream().use { it.readNBytes(2) }
            assertEquals("Not a JPEG: ${capture.imagePath}", listOf(0xFF.toByte(), 0xD8.toByte()), header.toList())
            assertTrue(capture.thumbnailPath?.let { File(it).isFile } == true)
        }
    }

    // 7
    @Test
    fun `demo images are all distinct`() = runBlocking {
        val project = loadDemo()
        val captures = db.captureDao().getCapturesListForProject(project.id)
        assertEquals(9, captures.map { it.imagePath }.toSet().size)
        val hashes = captures.map { c ->
            MessageDigest.getInstance("MD5").digest(File(c.imagePath).readBytes()).joinToString("") { "%02x".format(it) }
        }
        assertEquals(9, hashes.toSet().size)
    }

    // 8, 9, 10
    @Test
    fun `daily reports contain only that day's three shots`() = runBlocking {
        val project = loadDemo()
        for (dayNumber in 1..3) {
            val (file, text) = dailyReport(project, dayNumber)
            assertEquals("VFX_Project_Aurora_Day_%02d_Report.pdf".format(dayNumber), file.name)
            assertEquals("Day $dayNumber should list exactly 3 shots", 3, countOf(text, "SHOT 00"))
            for (scene in sceneNames.getValue(dayNumber)) assertTrue("Day $dayNumber missing $scene", text.contains(scene))
            for (scene in scenesOtherThan(dayNumber)) assertFalse("Day $dayNumber leaked $scene", text.contains(scene))
            assertTrue(text.contains("DEMO PROJECT"))
        }
    }

    // 11
    @Test
    fun `complete project report contains all 9 shots in every layout`() = runBlocking {
        val project = loadDemo()
        for (layout in listOf("Detailed", "Compact", "Image Only")) {
            val (file, text) = projectReport(project, layout)
            assertEquals("VFX_Project_Aurora_Project_Report.pdf", file.name)
            assertEquals(layout, 9, countOf(text, "SHOT 00"))
            for (scene in sceneNames.values.flatten()) assertTrue("$layout missing $scene", text.contains(scene))
            for (n in 1..3) assertTrue(text.contains("DAY $n"))
        }
    }

    @Test
    fun `daily reports render in all existing layouts`() = runBlocking {
        val project = loadDemo()
        for (layout in listOf("Detailed", "Compact", "Image Only")) {
            for (dayNumber in 1..3) {
                val (file, _) = dailyReport(project, dayNumber, layout)
                assertTrue(file.exists() && file.length() > 0)
            }
        }
    }

    @Test
    fun `loading the demo records the four named demo reports`() = runBlocking {
        val project = loadDemo()
        val reports = db.exportedReportDao().getReportsListForProject(project.id)
        assertEquals(
            setOf(
                "VFX_Project_Aurora_Day_01_Report.pdf",
                "VFX_Project_Aurora_Day_02_Report.pdf",
                "VFX_Project_Aurora_Day_03_Report.pdf",
                "VFX_Project_Aurora_Project_Report.pdf"
            ),
            reports.map { it.fileName }.toSet()
        )
        assertEquals(listOf(3, 3, 3, 9), reports.map { it.shotCount }.sorted())
        for (r in reports) assertTrue(File(r.filePath).isFile)
    }

    @Test
    fun `demo demonstrates project, day and shot inheritance`() = runBlocking {
        val project = loadDemo()
        val allDays = days(project.id)
        fun lens(dayNumber: Int, seq: Int): Pair<String, InheritanceSource> = runBlocking {
            val day = allDays.first { it.dayNumber == dayNumber }
            val capture = capturesOf(day)[seq - 1]
            val field = MetadataResolver.inspectCaptureCameraFields(capture, day, project).first { it.key == "focalLength" }
            field.value to field.source
        }
        for (seq in 1..3) assertEquals("35mm" to InheritanceSource.PROJECT_DEFAULT, lens(1, seq))
        for (seq in 1..3) assertEquals("85mm" to InheritanceSource.DAY_OVERRIDE, lens(2, seq))
        assertEquals("50mm" to InheritanceSource.DAY_OVERRIDE, lens(3, 1))
        assertEquals("50mm" to InheritanceSource.DAY_OVERRIDE, lens(3, 2))
        assertEquals("85mm" to InheritanceSource.SHOT_OVERRIDE, lens(3, 3))
    }

    @Test
    fun `demo captures carry the full data model`() = runBlocking {
        val project = loadDemo()
        val day1 = days(project.id).first()
        val shot1 = capturesOf(day1).first()
        assertEquals("SHOT 001", shot1.shotNumber)
        assertEquals("Reference", shot1.plateType)
        assertEquals("Interior Stage", shot1.environment)
        assertEquals("1.6 m", shot1.cameraHeight)
        assertEquals("3.5 m", shot1.subjectDistance)
        assertEquals("0°", shot1.cameraPan)
        assertEquals("-2°", shot1.cameraTilt)
        assertEquals("0°", shot1.cameraRoll)
        assertEquals("4 markers", shot1.trackingMarkers)
        assertEquals("Key light camera-left", shot1.lightingNotes)
        assertEquals("Establishing reference frame", shot1.vfxNotes)
        assertEquals("Demo Stage A", shot1.locationName)
        assertEquals("35mm", shot1.baselineCamera.focalLength)
        assertTrue(shot1.createdAt > 0)
        assertEquals("Demo Day 1", day1.date)
        assertEquals("Demo Stage A", day1.location)
    }

    // 12
    @Test
    fun `demo data never becomes defaults for new real projects`() = runBlocking {
        loadDemo()
        val real = ProjectEntity(name = "My Real Project")
        db.projectDao().insertProject(real)
        val loaded = db.projectDao().getProjectById(real.id)!!
        assertEquals(CameraSettings(), loaded.cameraDefaults)
        assertEquals(VfxSettings(), loaded.vfxDefaults)
        assertEquals("", loaded.client)
        assertEquals("", loaded.director)
        assertFalse(DemoProject.isDemo(loaded))

        val day = repository.createShootingDay(real.id)
        assertEquals("", day.location)
        assertFalse(day.cameraOverrides.hasAnyOverride())
        assertFalse(day.vfxOverrides.hasAnyOverride())

        val capture: CaptureEntity = repository.recordCapture(real.id, day.id, ByteArray(8))
        assertEquals(CameraSettings(), capture.baselineCamera)
        assertEquals(VfxSettings(), capture.baselineVfx)

        val (_, text) = projectReport(loaded)
        for (term in listOf("Sony", "FX6", "Zeiss", "Aurora", "DEMO", "S-Log3")) {
            assertFalse("Real report leaked '$term'", text.contains(term))
        }
    }

    // 13
    @Test
    fun `loading the demo twice does not create duplicates`() = runBlocking {
        val first = repository.loadDemoProject()
        val second = repository.loadDemoProject()
        assertFalse(first.alreadyLoaded)
        assertTrue(second.alreadyLoaded)
        assertEquals(first.project.id, second.project.id)
        assertEquals(1, db.projectDao().getProjectCount())
        assertEquals(3, days(first.project.id).size)
        assertEquals(9, db.captureDao().getCaptureCountForProject(first.project.id))
        assertEquals(4, db.exportedReportDao().getReportsListForProject(first.project.id).size)
    }

    // 14
    @Test
    fun `demo project can be deleted normally and reloaded`() = runBlocking {
        val project = loadDemo()
        val images = db.captureDao().getCapturesListForProject(project.id).map { File(it.imagePath) }
        val reports = db.exportedReportDao().getReportsListForProject(project.id).map { File(it.filePath) }

        repository.deleteProject(project)

        assertEquals(0, db.projectDao().getProjectCount())
        assertEquals(0, days(project.id).size)
        assertEquals(0, db.captureDao().getCaptureCountForProject(project.id))
        assertEquals(0, db.exportedReportDao().getReportsListForProject(project.id).size)
        assertTrue(images.none { it.exists() })
        assertTrue(reports.none { it.exists() })

        val again = repository.loadDemoProject()
        assertFalse(again.alreadyLoaded)
        assertEquals(1, db.projectDao().getProjectCount())
        assertNotNull(db.projectDao().getProjectByCode(DemoProject.CODE))
    }

    @Test
    fun `effective vfx metadata of every demo shot matches the shot's own fields`() = runBlocking {
        val project = loadDemo()
        for (capture in db.captureDao().getCapturesListForProject(project.id)) {
            val vfx = MetadataResolver.resolveEffectiveVfxSettings(capture.baselineVfx, capture.vfxOverrides)
            assertEquals(capture.plateType, vfx.plateType)
            assertEquals(capture.environment, vfx.environment)
            assertEquals(capture.cameraHeight, vfx.cameraHeight)
            assertEquals(capture.subjectDistance, vfx.defaultCameraDistance)
            assertEquals(capture.trackingMarkers, vfx.defaultTrackingNotes)
            assertEquals(capture.lightingNotes, vfx.lightingNotes)
        }
    }
}
