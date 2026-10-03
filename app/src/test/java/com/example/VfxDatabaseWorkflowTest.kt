package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.VfxDatabase
import com.example.data.model.CameraOverrides
import com.example.data.model.CameraSettings
import com.example.data.model.CaptureEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ShootingDayEntity
import com.example.data.model.VfxSettings
import com.example.pdf.PdfExportOptions
import com.example.pdf.VfxPdfReportGenerator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VfxDatabaseWorkflowTest {

    private lateinit var db: VfxDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, VfxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test complete VFX end-to-end database persistence and PDF report compilation`() = runBlocking {
        // 1. Create Project
        val project = ProjectEntity(
            name = "Project Falcon",
            client = "XYZ Studios",
            productionCompany = "Falcon Films",
            cameraDefaults = CameraSettings(
                cameraManufacturer = "Sony",
                cameraModel = "FX6",
                focalLength = "24mm",
                iso = "800",
                shutterSpeed = "1/48",
                aperture = "f/2.8",
                colorSpace = "S-Log3"
            ),
            vfxDefaults = VfxSettings(
                plateType = "Reference",
                cameraHeight = "1.5m",
                defaultCameraDistance = "2.8m"
            )
        )
        db.projectDao().insertProject(project)
        val loadedProject = db.projectDao().getProjectById(project.id)
        assertNotNull(loadedProject)
        assertEquals("Project Falcon", loadedProject?.name)

        // 2. Create Day 1
        val day1 = ShootingDayEntity(
            projectId = project.id,
            dayNumber = 1,
            date = "03 Oct 2026",
            location = "Pune, India",
            dayNotes = "Screen replacement reference passes"
        )
        db.shootingDayDao().insertShootingDay(day1)

        // 3. Create Shots
        val shot1 = CaptureEntity(
            projectId = project.id,
            shootingDayId = day1.id,
            shotNumber = "SHOT 001",
            shotSequenceNumber = 1,
            scene = "PC Screen",
            take = "Take 01",
            imagePath = "/dummy/shot1.jpg",
            baselineCamera = project.cameraDefaults,
            baselineVfx = project.vfxDefaults,
            shotNotes = "Screen replacement reference. Avoid reflections."
        )
        db.captureDao().insertCapture(shot1)

        val shot2 = CaptureEntity(
            projectId = project.id,
            shootingDayId = day1.id,
            shotNumber = "SHOT 002",
            shotSequenceNumber = 2,
            scene = "Close Up",
            take = "Take 01",
            imagePath = "/dummy/shot2.jpg",
            baselineCamera = project.cameraDefaults,
            baselineVfx = project.vfxDefaults,
            cameraOverrides = CameraOverrides(iso = "1600") // Shot override
        )
        db.captureDao().insertCapture(shot2)

        val dayCaptures = db.captureDao().getCapturesListForDay(day1.id)
        assertEquals(2, dayCaptures.size)
        assertEquals("SHOT 001", dayCaptures[0].shotNumber)
        assertEquals("SHOT 002", dayCaptures[1].shotNumber)

        // 4. Test PDF Generation
        val pdfFile = VfxPdfReportGenerator.generateReport(
            context = context,
            project = project,
            days = listOf(day1),
            capturesByDay = mapOf(day1.id to dayCaptures),
            options = PdfExportOptions(layoutType = "Detailed")
        )

        assertNotNull(pdfFile)
        assertTrue("PDF file should exist", pdfFile.exists())
        assertTrue("PDF file should be non-empty", pdfFile.length() > 0)
    }

    @Test
    fun `test empty first launch has zero projects`() = runBlocking {
        val projectCount = db.projectDao().getProjectCount()
        assertEquals("New database must start with zero projects", 0, projectCount)
    }

    @Test
    fun `test no fake report data on empty project`() = runBlocking {
        // Create an empty project without entering camera or client details
        val emptyProject = ProjectEntity(name = "Real Client Project")
        db.projectDao().insertProject(emptyProject)

        val pdfFile = VfxPdfReportGenerator.generateReport(
            context = context,
            project = emptyProject,
            days = emptyList(),
            capturesByDay = emptyMap(),
            options = PdfExportOptions(layoutType = "Detailed")
        )

        assertNotNull(pdfFile)
        assertTrue(pdfFile.exists())
        val textContent = pdfFile.readText(Charsets.ISO_8859_1)

        // Verify fabricated/sample values do NOT appear
        val fabricatedTerms = listOf("Sony", "FX6", "Zeiss", "800", "1/48", "f/2.8", "S-Log3", "Pune")
        for (term in fabricatedTerms) {
            assertTrue("PDF should not contain fabricated term '$term'", !textContent.contains(term))
        }
        assertTrue("PDF should indicate no shooting days", textContent.contains("No shooting days available"))
    }

    @Test
    fun `test daily report exports only selected day and its captures`() = runBlocking {
        // 1. Create Project
        val project = ProjectEntity(
            name = "Project ABC",
            client = "Warner VFX",
            cameraDefaults = CameraSettings(cameraManufacturer = "ARRI", cameraModel = "Alexa Mini", focalLength = "35mm", iso = "800")
        )
        db.projectDao().insertProject(project)

        // 2. Create Day 1 with 3 captures
        val day1 = ShootingDayEntity(projectId = project.id, dayNumber = 1, date = "01 Oct 2026", location = "Studio A")
        db.shootingDayDao().insertShootingDay(day1)
        val d1Shot1 = CaptureEntity(projectId = project.id, shootingDayId = day1.id, shotNumber = "SHOT 001", shotSequenceNumber = 1, baselineCamera = project.cameraDefaults)
        val d1Shot2 = CaptureEntity(projectId = project.id, shootingDayId = day1.id, shotNumber = "SHOT 002", shotSequenceNumber = 2, baselineCamera = project.cameraDefaults)
        val d1Shot3 = CaptureEntity(projectId = project.id, shootingDayId = day1.id, shotNumber = "SHOT 003", shotSequenceNumber = 3, baselineCamera = project.cameraDefaults)
        db.captureDao().insertCapture(d1Shot1)
        db.captureDao().insertCapture(d1Shot2)
        db.captureDao().insertCapture(d1Shot3)

        // 3. Create Day 2 with 2 captures
        val day2 = ShootingDayEntity(projectId = project.id, dayNumber = 2, date = "02 Oct 2026", location = "Backlot B")
        db.shootingDayDao().insertShootingDay(day2)
        val d2Shot1 = CaptureEntity(projectId = project.id, shootingDayId = day2.id, shotNumber = "SHOT 004", shotSequenceNumber = 4, baselineCamera = project.cameraDefaults)
        val d2Shot2 = CaptureEntity(projectId = project.id, shootingDayId = day2.id, shotNumber = "SHOT 005", shotSequenceNumber = 5, baselineCamera = project.cameraDefaults)
        db.captureDao().insertCapture(d2Shot1)
        db.captureDao().insertCapture(d2Shot2)

        val allDays = listOf(day1, day2)
        val capturesMap = mapOf(
            day1.id to listOf(d1Shot1, d1Shot2, d1Shot3),
            day2.id to listOf(d2Shot1, d2Shot2)
        )

        // 4. Export Daily Report for Day 2 ONLY
        val dailyPdf = VfxPdfReportGenerator.generateReport(
            context = context,
            project = project,
            days = allDays,
            capturesByDay = capturesMap,
            options = PdfExportOptions(selectedDayId = day2.id, layoutType = "Detailed")
        )

        assertNotNull(dailyPdf)
        assertTrue(dailyPdf.exists())
        assertEquals("VFX_Project_ABC_Day_02_Report.pdf", dailyPdf.name)

        val pdfContent = dailyPdf.readText(Charsets.ISO_8859_1)
        // Must contain Day 2 and its 2 captures
        assertTrue("Should contain Day 2", pdfContent.contains("Day 2"))
        assertTrue("Should contain SHOT 004", pdfContent.contains("SHOT 004"))
        assertTrue("Should contain SHOT 005", pdfContent.contains("SHOT 005"))

        // Must NOT contain Day 1 or Day 1 captures
        assertTrue("Should NOT contain Day 1", !pdfContent.contains("Day 1"))
        assertTrue("Should NOT contain SHOT 001", !pdfContent.contains("SHOT 001"))
        assertTrue("Should NOT contain SHOT 002", !pdfContent.contains("SHOT 002"))
        assertTrue("Should NOT contain SHOT 003", !pdfContent.contains("SHOT 003"))
    }

    @Test
    fun `test historical metadata preserved in daily report after project default changes`() = runBlocking {
        // Project starts at 24mm, ISO 800
        val project = ProjectEntity(
            name = "SciFi Plate",
            cameraDefaults = CameraSettings(focalLength = "24mm", iso = "800")
        )
        db.projectDao().insertProject(project)

        val day1 = ShootingDayEntity(projectId = project.id, dayNumber = 1, date = "01 Oct 2026")
        db.shootingDayDao().insertShootingDay(day1)

        val shot1 = CaptureEntity(
            projectId = project.id,
            shootingDayId = day1.id,
            shotNumber = "SHOT 001",
            shotSequenceNumber = 1,
            baselineCamera = project.cameraDefaults
        )
        db.captureDao().insertCapture(shot1)

        // Later, project defaults are changed to 85mm, ISO 3200
        val updatedProject = project.copy(
            cameraDefaults = CameraSettings(focalLength = "85mm", iso = "3200")
        )
        db.projectDao().updateProject(updatedProject)

        // Generate report using updated project
        val reportFile = VfxPdfReportGenerator.generateReport(
            context = context,
            project = updatedProject,
            days = listOf(day1),
            capturesByDay = mapOf(day1.id to listOf(shot1)),
            options = PdfExportOptions(selectedDayId = day1.id)
        )

        val content = reportFile.readText(Charsets.ISO_8859_1)
        // Historical shot metadata must still be 24mm and 800
        assertTrue("Shot 001 must keep historical 24mm lens", content.contains("24mm"))
        assertTrue("Shot 001 must keep historical ISO 800", content.contains("800"))
    }

    @Test
    fun `test complete project report exports all days and captures`() = runBlocking {
        val project = ProjectEntity(name = "Full Movie", client = "Studio Client")
        db.projectDao().insertProject(project)

        val day1 = ShootingDayEntity(projectId = project.id, dayNumber = 1, date = "01 Oct 2026")
        val day2 = ShootingDayEntity(projectId = project.id, dayNumber = 2, date = "02 Oct 2026")
        db.shootingDayDao().insertShootingDay(day1)
        db.shootingDayDao().insertShootingDay(day2)

        val shot1 = CaptureEntity(projectId = project.id, shootingDayId = day1.id, shotNumber = "SHOT 001", shotSequenceNumber = 1)
        val shot2 = CaptureEntity(projectId = project.id, shootingDayId = day2.id, shotNumber = "SHOT 002", shotSequenceNumber = 2)
        db.captureDao().insertCapture(shot1)
        db.captureDao().insertCapture(shot2)

        val pdf = VfxPdfReportGenerator.generateReport(
            context = context,
            project = project,
            days = listOf(day1, day2),
            capturesByDay = mapOf(day1.id to listOf(shot1), day2.id to listOf(shot2)),
            options = PdfExportOptions(selectedDayId = null) // Complete project
        )

        assertNotNull(pdf)
        assertTrue(pdf.exists())
        assertEquals("VFX_Full_Movie_Project_Report.pdf", pdf.name)

        val content = pdf.readText(Charsets.ISO_8859_1)
        assertTrue("Should contain Day 1", content.contains("DAY 1"))
        assertTrue("Should contain Day 2", content.contains("DAY 2"))
        assertTrue("Should contain SHOT 001", content.contains("SHOT 001"))
        assertTrue("Should contain SHOT 002", content.contains("SHOT 002"))
    }

    @Test
    fun `test brand new project camera and vfx defaults are empty and placeholders are not persisted`() = runBlocking {
        // Project created without entering Camera Defaults or VFX Defaults
        val emptyProj = ProjectEntity(name = "Clean Project")
        db.projectDao().insertProject(emptyProj)

        val retrieved = db.projectDao().getProjectById(emptyProj.id)
        assertNotNull(retrieved)

        val cam = retrieved!!.cameraDefaults
        val vfx = retrieved.vfxDefaults

        // Assert all camera fields are empty string
        assertEquals("", cam.cameraManufacturer)
        assertEquals("", cam.cameraModel)
        assertEquals("", cam.cameraUnitId)
        assertEquals("", cam.lensManufacturer)
        assertEquals("", cam.lensModel)
        assertEquals("", cam.lensId)
        assertEquals("", cam.focalLength)
        assertEquals("", cam.sensorFormat)
        assertEquals("", cam.sensorSize)
        assertEquals("", cam.resolution)
        assertEquals("", cam.frameRate)
        assertEquals("", cam.iso)
        assertEquals("", cam.shutterSpeed)
        assertEquals("", cam.aperture)
        assertEquals("", cam.whiteBalance)
        assertEquals("", cam.exposureCompensation)
        assertEquals("", cam.colorSpace)
        assertEquals("", cam.gammaProfile)
        assertEquals("", cam.recordingFormat)

        // Assert all VFX fields are empty string
        assertEquals("", vfx.plateType)
        assertEquals("", vfx.environment)
        assertEquals("", vfx.lightingNotes)
        assertEquals("", vfx.generalVfxNotes)
        assertEquals("", vfx.cameraHeight)
        assertEquals("", vfx.defaultCameraDistance)
        assertEquals("", vfx.defaultTrackingNotes)

        // Assert placeholders are NOT persisted
        val placeholderExamples = listOf("Sony FX6", "24mm", "800", "1/48", "f/2.8", "S-Log3", "1.6 m", "3.5 m")
        for (placeholder in placeholderExamples) {
            assertTrue("Database should not persist placeholder '$placeholder'", cam.cameraModel != placeholder)
            assertTrue("Database should not persist placeholder '$placeholder'", cam.focalLength != placeholder)
            assertTrue("Database should not persist placeholder '$placeholder'", cam.iso != placeholder)
        }
    }

    @Test
    fun `test entering single value persists only that field and all others remain empty`() = runBlocking {
        val proj = ProjectEntity(name = "Single Field Test")
        db.projectDao().insertProject(proj)

        // User enters only Lens = 35mm
        val updatedProj = proj.copy(
            cameraDefaults = proj.cameraDefaults.copy(focalLength = "35mm")
        )
        db.projectDao().updateProject(updatedProj)

        val loaded = db.projectDao().getProjectById(proj.id)
        assertNotNull(loaded)
        assertEquals("35mm", loaded!!.cameraDefaults.focalLength)

        // All other fields remain empty
        assertEquals("", loaded.cameraDefaults.cameraManufacturer)
        assertEquals("", loaded.cameraDefaults.cameraModel)
        assertEquals("", loaded.cameraDefaults.iso)
        assertEquals("", loaded.cameraDefaults.shutterSpeed)
        assertEquals("", loaded.cameraDefaults.aperture)
        assertEquals("", loaded.cameraDefaults.frameRate)
        assertEquals("", loaded.cameraDefaults.colorSpace)
        assertEquals("", loaded.vfxDefaults.plateType)
        assertEquals("", loaded.vfxDefaults.cameraHeight)
    }

    @Test
    fun `test clearing entered value returns field to empty`() = runBlocking {
        val proj = ProjectEntity(
            name = "Clear Field Test",
            cameraDefaults = CameraSettings(focalLength = "35mm")
        )
        db.projectDao().insertProject(proj)

        // User clears the field completely
        val clearedProj = proj.copy(
            cameraDefaults = proj.cameraDefaults.copy(focalLength = "")
        )
        db.projectDao().updateProject(clearedProj)

        val loaded = db.projectDao().getProjectById(proj.id)
        assertNotNull(loaded)
        assertEquals("", loaded!!.cameraDefaults.focalLength)
    }

    @Test
    fun `test empty project defaults produce empty day and shot inheritance without fake values`() = runBlocking {
        val emptyProj = ProjectEntity(name = "Inheritance Zero Test")
        db.projectDao().insertProject(emptyProj)

        val day = ShootingDayEntity(projectId = emptyProj.id, dayNumber = 1, date = "01 Oct 2026")
        db.shootingDayDao().insertShootingDay(day)

        // Inspect day resolved fields
        val dayFields = com.example.domain.inheritance.MetadataResolver.inspectDayCameraFields(emptyProj, day)
        for (field in dayFields) {
            assertEquals("Field '${field.label}' should be empty", "", field.value)
        }

        // Create shot inheriting from empty baseline
        val shot = CaptureEntity(
            projectId = emptyProj.id,
            shootingDayId = day.id,
            shotNumber = "SHOT 001",
            shotSequenceNumber = 1,
            baselineCamera = emptyProj.cameraDefaults
        )
        db.captureDao().insertCapture(shot)

        val effectiveCam = com.example.domain.inheritance.MetadataResolver.resolveEffectiveCameraSettings(
            shot.baselineCamera,
            shot.cameraOverrides
        )
        assertEquals("", effectiveCam.focalLength)
        assertEquals("", effectiveCam.iso)
        assertEquals("", effectiveCam.shutterSpeed)
        assertEquals("", effectiveCam.aperture)
    }

    @Test
    fun `test project report for empty project does not contain placeholder examples`() = runBlocking {
        val proj = ProjectEntity(name = "Empty Baseline Project", client = "Client Test")
        db.projectDao().insertProject(proj)

        val day = ShootingDayEntity(projectId = proj.id, dayNumber = 1, date = "01 Oct 2026")
        db.shootingDayDao().insertShootingDay(day)

        val shot = CaptureEntity(
            projectId = proj.id,
            shootingDayId = day.id,
            shotNumber = "SHOT 001",
            shotSequenceNumber = 1,
            baselineCamera = proj.cameraDefaults
        )
        db.captureDao().insertCapture(shot)

        val pdf = VfxPdfReportGenerator.generateReport(
            context = context,
            project = proj,
            days = listOf(day),
            capturesByDay = mapOf(day.id to listOf(shot)),
            options = PdfExportOptions(layoutType = "Detailed")
        )

        assertNotNull(pdf)
        assertTrue(pdf.exists())

        val content = pdf.readText(Charsets.ISO_8859_1)
        val placeholders = listOf("Sony FX6", "24mm", "ISO: 800", "1/48", "f/2.8", "S-Log3", "Pune")
        for (p in placeholders) {
            assertTrue("PDF must not contain placeholder '$p'", !content.contains(p))
        }
    }
}
