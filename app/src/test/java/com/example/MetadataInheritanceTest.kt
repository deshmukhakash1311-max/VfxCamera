package com.example

import com.example.data.model.CameraOverrides
import com.example.data.model.CameraSettings
import com.example.data.model.CaptureEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ShootingDayEntity
import com.example.domain.inheritance.MetadataResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MetadataInheritanceTest {

    @Test
    fun `test hierarchical inheritance rule - most specific value wins`() {
        // Level 1: Project Defaults
        val projectCamera = CameraSettings(
            cameraModel = "FX6",
            focalLength = "24mm",
            iso = "800",
            shutterSpeed = "1/48",
            aperture = "f/2.8",
            frameRate = "24 fps",
            colorSpace = "S-Log3"
        )
        val project = ProjectEntity(name = "Project Falcon", cameraDefaults = projectCamera)

        // Level 2: Day 1 inherits directly from Project
        val day1 = ShootingDayEntity(
            projectId = project.id,
            dayNumber = 1,
            date = "03 Oct 2026",
            cameraOverrides = CameraOverrides() // No overrides yet
        )

        val day1Effective = MetadataResolver.resolveDayCameraSettings(project.cameraDefaults, day1.cameraOverrides)
        assertEquals("800", day1Effective.iso)
        assertEquals("24mm", day1Effective.focalLength)
        assertEquals("f/2.8", day1Effective.aperture)

        // Capture 1 baseline under Day 1
        val (capture1Baseline, _) = MetadataResolver.buildBaselineForNewCapture(project, day1)
        val capture1Overrides = CameraOverrides()
        val capture1Effective = MetadataResolver.resolveEffectiveCameraSettings(capture1Baseline, capture1Overrides)
        assertEquals("800", capture1Effective.iso)
        assertEquals("24mm", capture1Effective.focalLength)

        // Now Day 1 overrides ISO to 1600
        val day1WithOverride = day1.copy(cameraOverrides = CameraOverrides(iso = "1600"))
        val day1UpdatedEffective = MetadataResolver.resolveDayCameraSettings(project.cameraDefaults, day1WithOverride.cameraOverrides)
        assertEquals("1600", day1UpdatedEffective.iso)
        assertEquals("800", project.cameraDefaults.iso) // Project remains 800!

        // New Capture 2 under Day 1 inherits 1600
        val (capture2Baseline, _) = MetadataResolver.buildBaselineForNewCapture(project, day1WithOverride)
        assertEquals("1600", capture2Baseline.iso)

        // Capture 2 sets a Shot Override to 3200
        val capture2Overrides = CameraOverrides(iso = "3200")
        val capture2Effective = MetadataResolver.resolveEffectiveCameraSettings(capture2Baseline, capture2Overrides)
        assertEquals("3200", capture2Effective.iso)
        assertEquals("1600", day1UpdatedEffective.iso) // Day remains 1600!
        assertEquals("800", project.cameraDefaults.iso) // Project remains 800!
    }

    @Test
    fun `test historical capture preservation - editing project later does not alter historical captures`() {
        // Given Project with 24mm lens
        var project = ProjectEntity(
            name = "Project Falcon",
            cameraDefaults = CameraSettings(focalLength = "24mm", iso = "800")
        )
        val day1 = ShootingDayEntity(projectId = project.id, dayNumber = 1, date = "03 Oct 2026")

        // Capture 1 frozen at capture time
        val (c1Baseline, c1Vfx) = MetadataResolver.buildBaselineForNewCapture(project, day1)
        val shot1 = CaptureEntity(
            projectId = project.id,
            shootingDayId = day1.id,
            shotNumber = "SHOT 001",
            shotSequenceNumber = 1,
            imagePath = "/fake/shot1.jpg",
            baselineCamera = c1Baseline,
            baselineVfx = c1Vfx
        )

        // Verify initial effective lens
        val initialEffective = MetadataResolver.resolveEffectiveCameraSettings(shot1.baselineCamera, shot1.cameraOverrides)
        assertEquals("24mm", initialEffective.focalLength)

        // Now user changes Project default lens to 50mm later in the shoot
        project = project.copy(cameraDefaults = project.cameraDefaults.copy(focalLength = "50mm"))

        // Historical Shot 1 MUST preserve its original 24mm reading
        val historicalEffective = MetadataResolver.resolveEffectiveCameraSettings(shot1.baselineCamera, shot1.cameraOverrides)
        assertEquals("24mm", historicalEffective.focalLength)
    }

    @Test
    fun `test day lens override and new day isolation`() {
        val project = ProjectEntity(
            name = "Project Falcon",
            cameraDefaults = CameraSettings(focalLength = "24mm", iso = "800")
        )

        // Day 1 uses project default 24mm
        val day1 = ShootingDayEntity(projectId = project.id, dayNumber = 1, date = "03 Oct 2026")

        // Day 2 overrides lens to 50mm
        val day2 = ShootingDayEntity(
            projectId = project.id,
            dayNumber = 2,
            date = "04 Oct 2026",
            cameraOverrides = CameraOverrides(focalLength = "50mm")
        )

        val day1Cam = MetadataResolver.resolveDayCameraSettings(project.cameraDefaults, day1.cameraOverrides)
        val day2Cam = MetadataResolver.resolveDayCameraSettings(project.cameraDefaults, day2.cameraOverrides)

        assertEquals("24mm", day1Cam.focalLength)
        assertEquals("50mm", day2Cam.focalLength)
        assertEquals("24mm", project.cameraDefaults.focalLength)
    }
}
