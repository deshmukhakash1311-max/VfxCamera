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
}
