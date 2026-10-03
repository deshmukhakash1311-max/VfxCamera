package com.example.data.demo

import com.example.data.model.CameraOverrides
import com.example.data.model.CameraSettings
import com.example.data.model.CaptureEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ShootingDayEntity
import com.example.data.model.VfxOverrides
import com.example.data.model.VfxSettings
import com.example.domain.inheritance.MetadataResolver

/**
 * Definition of the optional, explicitly loaded "Project Aurora" demo project.
 *
 * Nothing here is ever applied automatically and none of these values are defaults for real
 * projects: [ProjectEntity], [CameraSettings] and [VfxSettings] still default to empty strings.
 * Everything is built into normal Room entities so the demo uses the same persistence path as
 * real data.
 */
object DemoProject {
    const val NAME = "Project Aurora"
    const val CODE = "DEMO-AURORA"
    const val BADGE = "DEMO PROJECT"
    const val ASSET_DIR = "demo"

    fun isDemo(project: ProjectEntity): Boolean = project.projectIdCode == CODE

    class ShotSpec(
        val shot: CaptureEntity,
        val assetName: String
    )

    class DaySpec(
        val day: ShootingDayEntity,
        val shots: List<ShotSpec>
    )

    class Spec(
        val project: ProjectEntity,
        val days: List<DaySpec>
    )

    private val cameraDefaults = CameraSettings(
        cameraManufacturer = "Sony",
        cameraModel = "FX6",
        cameraUnitId = "A-CAM",
        lensManufacturer = "Zeiss",
        lensModel = "Supreme Prime",
        lensId = "SP-35",
        focalLength = "35mm",
        sensorFormat = "Full Frame 35mm",
        sensorSize = "35.6 x 23.8 mm",
        resolution = "3840×2160",
        frameRate = "24 fps",
        iso = "800",
        shutterSpeed = "1/48",
        aperture = "f/2.8",
        whiteBalance = "5600K",
        exposureCompensation = "0 EV",
        colorSpace = "S-Log3",
        gammaProfile = "S-Log3",
        recordingFormat = "XAVC-I 422 10-bit"
    )

    private val vfxDefaults = VfxSettings(
        plateType = "Reference",
        environment = "Interior Stage",
        lightingNotes = "Key light camera-left",
        generalVfxNotes = "Demonstration project — sample data",
        cameraHeight = "1.6 m",
        defaultCameraDistance = "3.5 m",
        defaultTrackingNotes = "4 markers"
    )

    private class ShotDef(
        val scene: String,
        val subject: String,
        val plateType: String,
        val environment: String,
        val cameraHeight: String,
        val subjectDistance: String,
        val pan: String,
        val tilt: String,
        val roll: String,
        val markers: String,
        val lighting: String,
        val vfxNotes: String,
        val bgFg: String,
        val notes: String,
        val cameraOverrides: CameraOverrides = CameraOverrides(),
        val vfxOverrides: VfxOverrides = VfxOverrides()
    )

    private val day1Shots = listOf(
        ShotDef(
            scene = "Stage Establishing", subject = "Stage wide / hero set",
            plateType = "Reference", environment = "Interior Stage",
            cameraHeight = "1.6 m", subjectDistance = "3.5 m",
            pan = "0°", tilt = "-2°", roll = "0°",
            markers = "4 markers", lighting = "Key light camera-left",
            vfxNotes = "Establishing reference frame",
            bgFg = "BG: stage cyc / FG: empty floor",
            notes = "Establishing frame for the set. Hold for 10 seconds."
        ),
        ShotDef(
            scene = "Green Screen Plate", subject = "Green screen wall",
            plateType = "Green Screen", environment = "Interior Stage",
            cameraHeight = "1.5 m", subjectDistance = "3.0 m",
            pan = "15°", tilt = "0°", roll = "0°",
            markers = "8 markers (white)", lighting = "Even soft light on screen",
            vfxNotes = "Check spill at floor edge",
            bgFg = "BG: green screen / FG: floor",
            notes = "Aperture closed down to f/4 for this setup (shot override).",
            cameraOverrides = CameraOverrides(aperture = "f/4"),
            vfxOverrides = VfxOverrides(plateType = "Green Screen")
        ),
        ShotDef(
            scene = "Clean Plate", subject = "Empty stage",
            plateType = "Reference", environment = "Interior Stage",
            cameraHeight = "1.6 m", subjectDistance = "3.5 m",
            pan = "0°", tilt = "-2°", roll = "0°",
            markers = "4 markers", lighting = "Same as establishing frame",
            vfxNotes = "Clean plate for paint-out",
            bgFg = "BG: stage cyc / FG: none",
            notes = "Clean plate, no talent or props in frame."
        )
    )

    private val day2Shots = listOf(
        ShotDef(
            scene = "Marker Wall", subject = "Tracking marker wall",
            plateType = "Tracking Plate", environment = "Interior Stage B",
            cameraHeight = "1.4 m", subjectDistance = "2.0 m",
            pan = "-10°", tilt = "0°", roll = "0°",
            markers = "40 markers (orange / white grid)", lighting = "Flat practical lighting",
            vfxNotes = "Tracking reference for camera solve",
            bgFg = "BG: marker wall / FG: none",
            notes = "Marker grid for lens and camera solve. Inherits the 85mm day lens."
        ),
        ShotDef(
            scene = "Foreground Prop", subject = "Hero prop on green",
            plateType = "Foreground Element", environment = "Interior Stage B",
            cameraHeight = "1.2 m", subjectDistance = "2.5 m",
            pan = "5°", tilt = "3°", roll = "0°",
            markers = "6 markers", lighting = "Key camera-right, even green screen",
            vfxNotes = "Prop needs roto; edge detail on handle",
            bgFg = "BG: green screen / FG: hero prop",
            notes = "Hero prop isolated for roto.",
            vfxOverrides = VfxOverrides(plateType = "Foreground Element")
        ),
        ShotDef(
            scene = "Lighting Rig", subject = "Light and camera positions",
            plateType = "Tracking Plate", environment = "Interior Stage B",
            cameraHeight = "1.8 m", subjectDistance = "4.0 m",
            pan = "0°", tilt = "-5°", roll = "0°",
            markers = "None", lighting = "3-point: key, fill and back documented",
            vfxNotes = "Light positions for CG relight",
            bgFg = "BG: stage / FG: rig",
            notes = "Lighting diagram reference for CG relight."
        )
    )

    private val day3Shots = listOf(
        ShotDef(
            scene = "Skyline", subject = "Exterior skyline",
            plateType = "Environment Reference", environment = "Exterior Set",
            cameraHeight = "1.7 m", subjectDistance = "Far field",
            pan = "30°", tilt = "5°", roll = "0°",
            markers = "None", lighting = "Golden hour, sun camera-right",
            vfxNotes = "Skyline extension reference",
            bgFg = "BG: skyline / FG: grass",
            notes = "Environment reference for skyline extension."
        ),
        ShotDef(
            scene = "Chrome Ball", subject = "Grey and chrome ball",
            plateType = "Environment Reference", environment = "Exterior Set",
            cameraHeight = "1.0 m", subjectDistance = "2.0 m",
            pan = "0°", tilt = "0°", roll = "0°",
            markers = "None", lighting = "Soft sun with overcast fill",
            vfxNotes = "Grey and chrome ball for lighting match",
            bgFg = "BG: sky / FG: reference balls",
            notes = "Lighting reference for HDRI and CG match."
        ),
        ShotDef(
            scene = "Sun Position", subject = "Talent mark in sun",
            plateType = "Environment Reference", environment = "Exterior Set",
            cameraHeight = "1.6 m", subjectDistance = "6.0 m",
            pan = "-20°", tilt = "2°", roll = "0°",
            markers = "2 markers", lighting = "Hard sun, about 40° elevation",
            vfxNotes = "Sun direction reference for the CG relight",
            bgFg = "BG: skyline / FG: talent mark",
            notes = "Shot-specific 85mm lens override on top of the 50mm day lens.",
            cameraOverrides = CameraOverrides(focalLength = "85mm", lensId = "SP-85")
        )
    )

    /**
     * Builds fresh, unsaved entities. [imagePathFor] maps (dayNumber, shotNumber) to the local
     * image path the capture should point at.
     */
    fun build(
        now: Long = System.currentTimeMillis(),
        imagePathFor: (dayNumber: Int, shotNumber: Int) -> String
    ): Spec {
        val project = ProjectEntity(
            name = NAME,
            client = "Aurora Studios",
            productionCompany = "Aurora Studios",
            productionShow = "VFX / Virtual Production — $BADGE",
            projectIdCode = CODE,
            description = "$BADGE. Demonstration project — sample data",
            director = "Demo Director",
            vfxSupervisor = "Demo VFX Supervisor",
            vfxProducer = "Demo VFX Producer",
            cameraOperator = "Demo Camera Operator",
            date = "Sample dates",
            location = "Demo Stage A / Demo Stage B / Demo Exterior Set",
            createdAt = now,
            updatedAt = now,
            cameraDefaults = cameraDefaults,
            vfxDefaults = vfxDefaults
        )

        data class DayDef(
            val number: Int,
            val date: String,
            val location: String,
            val notes: String,
            val camera: CameraOverrides,
            val vfx: VfxOverrides,
            val shots: List<ShotDef>
        )

        val dayDefs = listOf(
            DayDef(
                1, "Demo Day 1", "Demo Stage A", "Establishing and clean plate reference",
                CameraOverrides(), VfxOverrides(), day1Shots
            ),
            DayDef(
                2, "Demo Day 2", "Demo Stage B", "Tracking and foreground reference",
                CameraOverrides(focalLength = "85mm", lensId = "SP-85"),
                VfxOverrides(plateType = "Tracking Plate", environment = "Interior Stage B", cameraHeight = "1.4 m"),
                day2Shots
            ),
            DayDef(
                3, "Demo Day 3", "Demo Exterior Set", "Environment and lighting reference",
                CameraOverrides(
                    focalLength = "50mm", lensId = "SP-50",
                    iso = "400", aperture = "f/5.6", whiteBalance = "5200K"
                ),
                VfxOverrides(
                    plateType = "Environment Reference", environment = "Exterior Set",
                    lightingNotes = "Natural sun, about 5200K"
                ),
                day3Shots
            )
        )

        val days = dayDefs.map { d ->
            val dayStart = now - (3 - d.number) * DAY_MS
            val day = ShootingDayEntity(
                projectId = project.id,
                dayNumber = d.number,
                date = d.date,
                location = d.location,
                dayNotes = d.notes,
                createdAt = dayStart,
                updatedAt = dayStart,
                cameraOverrides = d.camera,
                vfxOverrides = d.vfx
            )
            val shots = d.shots.mapIndexed { i, s ->
                val seq = i + 1
                // Same inheritance path as a real capture: project defaults + day overrides are
                // frozen into the baseline.
                val (baseCam, baseVfx) = MetadataResolver.buildBaselineForNewCapture(project, day)
                val takenAt = dayStart + (9 * 60 + seq * 25) * 60_000L
                val imagePath = imagePathFor(d.number, seq)
                ShotSpec(
                    shot = CaptureEntity(
                        projectId = project.id,
                        shootingDayId = day.id,
                        shotNumber = MetadataResolver.formatShotNumber(seq),
                        shotSequenceNumber = seq,
                        scene = s.scene,
                        take = "Take 01",
                        subject = s.subject,
                        description = "$BADGE sample capture",
                        imagePath = imagePath,
                        thumbnailPath = imagePath,
                        isReference = seq == 1,
                        createdAt = takenAt,
                        updatedAt = takenAt,
                        baselineCamera = baseCam,
                        baselineVfx = baseVfx,
                        cameraOverrides = s.cameraOverrides,
                        vfxOverrides = withShotValues(s, baseVfx),
                        shotNotes = s.notes,
                        additionalNotes = "$BADGE — sample data",
                        locationName = d.location,
                        plateType = s.plateType,
                        environment = s.environment,
                        cameraHeight = s.cameraHeight,
                        subjectDistance = s.subjectDistance,
                        cameraDistance = s.subjectDistance,
                        cameraPan = s.pan,
                        cameraTilt = s.tilt,
                        cameraRoll = s.roll,
                        trackingMarkers = s.markers,
                        lightingNotes = s.lighting,
                        vfxNotes = s.vfxNotes,
                        referenceNotes = "Sample reference image generated for demonstration",
                        bgFgInfo = s.bgFg
                    ),
                    assetName = assetName(d.number, seq)
                )
            }
            DaySpec(day, shots)
        }
        return Spec(project, days)
    }

    /**
     * Shot-level VFX overrides: any per-shot value that differs from the inherited (project + day)
     * baseline is stored as a shot override, so the effective metadata matches the shot's own
     * fields instead of showing a contradicting inherited value.
     */
    private fun withShotValues(s: ShotDef, base: VfxSettings): VfxOverrides {
        fun diff(shotValue: String, inherited: String): String? =
            shotValue.takeIf { it.isNotBlank() && it != inherited }
        val explicit = s.vfxOverrides
        return VfxOverrides(
            plateType = explicit.plateType ?: diff(s.plateType, base.plateType),
            environment = explicit.environment ?: diff(s.environment, base.environment),
            lightingNotes = explicit.lightingNotes ?: diff(s.lighting, base.lightingNotes),
            generalVfxNotes = explicit.generalVfxNotes,
            cameraHeight = explicit.cameraHeight ?: diff(s.cameraHeight, base.cameraHeight),
            defaultCameraDistance = explicit.defaultCameraDistance ?: diff(s.subjectDistance, base.defaultCameraDistance),
            defaultTrackingNotes = explicit.defaultTrackingNotes ?: diff(s.markers, base.defaultTrackingNotes)
        )
    }

    fun assetName(dayNumber: Int, shotNumber: Int): String =
        "day%02d_shot%03d.jpg".format(dayNumber, shotNumber)

    private const val DAY_MS = 24L * 60 * 60 * 1000
}
