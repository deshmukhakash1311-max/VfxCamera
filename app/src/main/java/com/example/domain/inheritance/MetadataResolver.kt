package com.example.domain.inheritance

import com.example.data.model.CameraOverrides
import com.example.data.model.CameraSettings
import com.example.data.model.CaptureEntity
import com.example.data.model.InheritanceSource
import com.example.data.model.ProjectEntity
import com.example.data.model.ResolvedField
import com.example.data.model.ShootingDayEntity
import com.example.data.model.VfxOverrides
import com.example.data.model.VfxSettings

object MetadataResolver {

    fun formatShotNumber(sequence: Int): String {
        return "SHOT %03d".format(sequence)
    }

    /**
     * Resolve effective camera settings for a shooting day:
     * Project defaults overridden by Day overrides.
     */
    fun resolveDayCameraSettings(
        projectCamera: CameraSettings,
        dayOverrides: CameraOverrides
    ): CameraSettings {
        return CameraSettings(
            cameraManufacturer = dayOverrides.cameraManufacturer ?: projectCamera.cameraManufacturer,
            cameraModel = dayOverrides.cameraModel ?: projectCamera.cameraModel,
            cameraUnitId = dayOverrides.cameraUnitId ?: projectCamera.cameraUnitId,
            lensManufacturer = dayOverrides.lensManufacturer ?: projectCamera.lensManufacturer,
            lensModel = dayOverrides.lensModel ?: projectCamera.lensModel,
            lensId = dayOverrides.lensId ?: projectCamera.lensId,
            focalLength = dayOverrides.focalLength ?: projectCamera.focalLength,
            sensorFormat = dayOverrides.sensorFormat ?: projectCamera.sensorFormat,
            sensorSize = dayOverrides.sensorSize ?: projectCamera.sensorSize,
            resolution = dayOverrides.resolution ?: projectCamera.resolution,
            frameRate = dayOverrides.frameRate ?: projectCamera.frameRate,
            iso = dayOverrides.iso ?: projectCamera.iso,
            shutterSpeed = dayOverrides.shutterSpeed ?: projectCamera.shutterSpeed,
            aperture = dayOverrides.aperture ?: projectCamera.aperture,
            whiteBalance = dayOverrides.whiteBalance ?: projectCamera.whiteBalance,
            exposureCompensation = dayOverrides.exposureCompensation ?: projectCamera.exposureCompensation,
            colorSpace = dayOverrides.colorSpace ?: projectCamera.colorSpace,
            gammaProfile = dayOverrides.gammaProfile ?: projectCamera.gammaProfile,
            recordingFormat = dayOverrides.recordingFormat ?: projectCamera.recordingFormat
        )
    }

    /**
     * Resolve effective VFX settings for a shooting day.
     */
    fun resolveDayVfxSettings(
        projectVfx: VfxSettings,
        dayOverrides: VfxOverrides
    ): VfxSettings {
        return VfxSettings(
            plateType = dayOverrides.plateType ?: projectVfx.plateType,
            environment = dayOverrides.environment ?: projectVfx.environment,
            lightingNotes = dayOverrides.lightingNotes ?: projectVfx.lightingNotes,
            generalVfxNotes = dayOverrides.generalVfxNotes ?: projectVfx.generalVfxNotes,
            cameraHeight = dayOverrides.cameraHeight ?: projectVfx.cameraHeight,
            defaultCameraDistance = dayOverrides.defaultCameraDistance ?: projectVfx.defaultCameraDistance,
            defaultTrackingNotes = dayOverrides.defaultTrackingNotes ?: projectVfx.defaultTrackingNotes
        )
    }

    /**
     * Resolve effective camera settings for a capture:
     * Baseline (frozen at capture time) overridden by shot-level overrides.
     */
    fun resolveEffectiveCameraSettings(
        baseline: CameraSettings,
        shotOverrides: CameraOverrides
    ): CameraSettings {
        return CameraSettings(
            cameraManufacturer = shotOverrides.cameraManufacturer ?: baseline.cameraManufacturer,
            cameraModel = shotOverrides.cameraModel ?: baseline.cameraModel,
            cameraUnitId = shotOverrides.cameraUnitId ?: baseline.cameraUnitId,
            lensManufacturer = shotOverrides.lensManufacturer ?: baseline.lensManufacturer,
            lensModel = shotOverrides.lensModel ?: baseline.lensModel,
            lensId = shotOverrides.lensId ?: baseline.lensId,
            focalLength = shotOverrides.focalLength ?: baseline.focalLength,
            sensorFormat = shotOverrides.sensorFormat ?: baseline.sensorFormat,
            sensorSize = shotOverrides.sensorSize ?: baseline.sensorSize,
            resolution = shotOverrides.resolution ?: baseline.resolution,
            frameRate = shotOverrides.frameRate ?: baseline.frameRate,
            iso = shotOverrides.iso ?: baseline.iso,
            shutterSpeed = shotOverrides.shutterSpeed ?: baseline.shutterSpeed,
            aperture = shotOverrides.aperture ?: baseline.aperture,
            whiteBalance = shotOverrides.whiteBalance ?: baseline.whiteBalance,
            exposureCompensation = shotOverrides.exposureCompensation ?: baseline.exposureCompensation,
            colorSpace = shotOverrides.colorSpace ?: baseline.colorSpace,
            gammaProfile = shotOverrides.gammaProfile ?: baseline.gammaProfile,
            recordingFormat = shotOverrides.recordingFormat ?: baseline.recordingFormat
        )
    }

    /**
     * Resolve effective VFX settings for a capture.
     */
    fun resolveEffectiveVfxSettings(
        baseline: VfxSettings,
        shotOverrides: VfxOverrides
    ): VfxSettings {
        return VfxSettings(
            plateType = shotOverrides.plateType ?: baseline.plateType,
            environment = shotOverrides.environment ?: baseline.environment,
            lightingNotes = shotOverrides.lightingNotes ?: baseline.lightingNotes,
            generalVfxNotes = shotOverrides.generalVfxNotes ?: baseline.generalVfxNotes,
            cameraHeight = shotOverrides.cameraHeight ?: baseline.cameraHeight,
            defaultCameraDistance = shotOverrides.defaultCameraDistance ?: baseline.defaultCameraDistance,
            defaultTrackingNotes = shotOverrides.defaultTrackingNotes ?: baseline.defaultTrackingNotes
        )
    }

    /**
     * Build the baseline metadata for a new capture created right now.
     * Takes Project defaults and Day overrides.
     */
    fun buildBaselineForNewCapture(
        project: ProjectEntity,
        day: ShootingDayEntity
    ): Pair<CameraSettings, VfxSettings> {
        val cam = resolveDayCameraSettings(project.cameraDefaults, day.cameraOverrides)
        val vfx = resolveDayVfxSettings(project.vfxDefaults, day.vfxOverrides)
        return Pair(cam, vfx)
    }

    /**
     * Generate list of resolved camera fields for Day Details display,
     * highlighting whether each field is inherited from Project or overridden by Day.
     */
    fun inspectDayCameraFields(
        project: ProjectEntity,
        day: ShootingDayEntity
    ): List<ResolvedField> {
        val d = day.cameraOverrides
        val p = project.cameraDefaults

        fun check(key: String, label: String, dayVal: String?, projVal: String): ResolvedField {
            val isOverridden = dayVal != null && dayVal.isNotBlank()
            return ResolvedField(
                key = key,
                label = label,
                value = if (isOverridden) dayVal!! else projVal,
                source = if (isOverridden) InheritanceSource.DAY_OVERRIDE else InheritanceSource.PROJECT_DEFAULT,
                sourceDescription = if (isOverridden) "Day Override" else "Inherited from Project"
            )
        }

        return listOf(
            check("cameraManufacturer", "Camera Manufacturer", d.cameraManufacturer, p.cameraManufacturer),
            check("cameraModel", "Camera Model", d.cameraModel, p.cameraModel),
            check("cameraUnitId", "Camera Unit ID", d.cameraUnitId, p.cameraUnitId),
            check("lensManufacturer", "Lens Manufacturer", d.lensManufacturer, p.lensManufacturer),
            check("lensModel", "Lens Model", d.lensModel, p.lensModel),
            check("lensId", "Lens ID", d.lensId, p.lensId),
            check("focalLength", "Focal Length", d.focalLength, p.focalLength),
            check("sensorFormat", "Sensor Format", d.sensorFormat, p.sensorFormat),
            check("sensorSize", "Sensor Size", d.sensorSize, p.sensorSize),
            check("resolution", "Resolution", d.resolution, p.resolution),
            check("frameRate", "Frame Rate", d.frameRate, p.frameRate),
            check("iso", "ISO", d.iso, p.iso),
            check("shutterSpeed", "Shutter Speed", d.shutterSpeed, p.shutterSpeed),
            check("aperture", "Aperture", d.aperture, p.aperture),
            check("whiteBalance", "White Balance", d.whiteBalance, p.whiteBalance),
            check("exposureCompensation", "Exposure Comp", d.exposureCompensation, p.exposureCompensation),
            check("colorSpace", "Color Space", d.colorSpace, p.colorSpace),
            check("gammaProfile", "Gamma / Log", d.gammaProfile, p.gammaProfile),
            check("recordingFormat", "Recording Format", d.recordingFormat, p.recordingFormat)
        )
    }

    /**
     * Generate list of resolved camera fields for a Capture,
     * showing exact provenance (Shot Override, Day Override, or Project Default),
     * and displaying original camera sensor readings when available.
     */
    fun inspectCaptureCameraFields(
        capture: CaptureEntity,
        day: ShootingDayEntity?,
        project: ProjectEntity?
    ): List<ResolvedField> {
        val s = capture.cameraOverrides
        val b = capture.baselineCamera

        fun resolveField(
            key: String,
            label: String,
            shotOverride: String?,
            baselineVal: String,
            dayOverride: String?,
            originalVal: String? = null
        ): ResolvedField {
            val hasShotOverride = shotOverride != null && shotOverride.isNotBlank()
            val effectiveVal = if (hasShotOverride) shotOverride!! else baselineVal

            val source: InheritanceSource
            val desc: String

            if (hasShotOverride) {
                source = InheritanceSource.SHOT_OVERRIDE
                desc = "Shot Override"
            } else if (dayOverride != null && dayOverride.isNotBlank()) {
                source = InheritanceSource.DAY_OVERRIDE
                desc = "Inherited from Day ${day?.dayNumber ?: 1}"
            } else {
                source = InheritanceSource.PROJECT_DEFAULT
                desc = "Inherited from Project"
            }

            return ResolvedField(
                key = key,
                label = label,
                value = effectiveVal,
                source = source,
                sourceDescription = desc,
                originalCameraValue = originalVal
            )
        }

        val d = day?.cameraOverrides
        return listOf(
            resolveField("cameraManufacturer", "Camera Manufacturer", s.cameraManufacturer, b.cameraManufacturer, d?.cameraManufacturer),
            resolveField("cameraModel", "Camera Model", s.cameraModel, b.cameraModel, d?.cameraModel),
            resolveField("cameraUnitId", "Camera Unit ID", s.cameraUnitId, b.cameraUnitId, d?.cameraUnitId),
            resolveField("lensManufacturer", "Lens Manufacturer", s.lensManufacturer, b.lensManufacturer, d?.lensManufacturer),
            resolveField("lensModel", "Lens Model", s.lensModel, b.lensModel, d?.lensModel, capture.originalCameraLensModel),
            resolveField("lensId", "Lens ID", s.lensId, b.lensId, d?.lensId),
            resolveField("focalLength", "Focal Length", s.focalLength, b.focalLength, d?.focalLength, capture.originalCameraFocalLength),
            resolveField("sensorFormat", "Sensor Format", s.sensorFormat, b.sensorFormat, d?.sensorFormat),
            resolveField("sensorSize", "Sensor Size", s.sensorSize, b.sensorSize, d?.sensorSize),
            resolveField("resolution", "Resolution", s.resolution, b.resolution, d?.resolution, capture.originalCameraResolution),
            resolveField("frameRate", "Frame Rate", s.frameRate, b.frameRate, d?.frameRate),
            resolveField("iso", "ISO", s.iso, b.iso, d?.iso, capture.originalCameraIso),
            resolveField("shutterSpeed", "Shutter Speed", s.shutterSpeed, b.shutterSpeed, d?.shutterSpeed, capture.originalCameraShutter),
            resolveField("aperture", "Aperture", s.aperture, b.aperture, d?.aperture, capture.originalCameraAperture),
            resolveField("whiteBalance", "White Balance", s.whiteBalance, b.whiteBalance, d?.whiteBalance, capture.originalCameraWhiteBalance),
            resolveField("exposureCompensation", "Exposure Comp", s.exposureCompensation, b.exposureCompensation, d?.exposureCompensation, capture.originalCameraExposureComp),
            resolveField("colorSpace", "Color Space", s.colorSpace, b.colorSpace, d?.colorSpace),
            resolveField("gammaProfile", "Gamma / Log", s.gammaProfile, b.gammaProfile, d?.gammaProfile),
            resolveField("recordingFormat", "Recording Format", s.recordingFormat, b.recordingFormat, d?.recordingFormat)
        )
    }

    /**
     * Inspect VFX fields for a Capture.
     */
    fun inspectCaptureVfxFields(
        capture: CaptureEntity,
        day: ShootingDayEntity?,
        project: ProjectEntity?
    ): List<ResolvedField> {
        val s = capture.vfxOverrides
        val b = capture.baselineVfx
        val d = day?.vfxOverrides

        fun resolveField(
            key: String,
            label: String,
            shotOverride: String?,
            baselineVal: String,
            dayOverride: String?
        ): ResolvedField {
            val hasShotOverride = shotOverride != null && shotOverride.isNotBlank()
            val effectiveVal = if (hasShotOverride) shotOverride!! else baselineVal

            val source: InheritanceSource
            val desc: String

            if (hasShotOverride) {
                source = InheritanceSource.SHOT_OVERRIDE
                desc = "Shot Override"
            } else if (dayOverride != null && dayOverride.isNotBlank()) {
                source = InheritanceSource.DAY_OVERRIDE
                desc = "Inherited from Day ${day?.dayNumber ?: 1}"
            } else {
                source = InheritanceSource.PROJECT_DEFAULT
                desc = "Inherited from Project"
            }

            return ResolvedField(
                key = key,
                label = label,
                value = effectiveVal,
                source = source,
                sourceDescription = desc
            )
        }

        return listOf(
            resolveField("plateType", "Plate Type", s.plateType ?: capture.plateType.takeIf { it.isNotBlank() }, b.plateType, d?.plateType),
            resolveField("environment", "Environment", s.environment ?: capture.environment.takeIf { it.isNotBlank() }, b.environment, d?.environment),
            resolveField("cameraHeight", "Camera Height", s.cameraHeight ?: capture.cameraHeight.takeIf { it.isNotBlank() }, b.cameraHeight, d?.cameraHeight),
            resolveField("cameraDistance", "Camera Distance", s.defaultCameraDistance ?: capture.cameraDistance.takeIf { it.isNotBlank() }, b.defaultCameraDistance, d?.defaultCameraDistance),
            resolveField("lightingNotes", "Lighting Notes", s.lightingNotes ?: capture.lightingNotes.takeIf { it.isNotBlank() }, b.lightingNotes, d?.lightingNotes),
            resolveField("trackingMarkers", "Tracking Markers", s.defaultTrackingNotes ?: capture.trackingMarkers.takeIf { it.isNotBlank() }, b.defaultTrackingNotes, d?.defaultTrackingNotes),
            resolveField("vfxNotes", "General VFX Notes", s.generalVfxNotes ?: capture.vfxNotes.takeIf { it.isNotBlank() }, b.generalVfxNotes, d?.generalVfxNotes)
        )
    }
}
