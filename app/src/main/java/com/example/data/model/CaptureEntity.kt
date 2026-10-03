package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "captures",
    foreignKeys = [
        ForeignKey(
            entity = ShootingDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["shootingDayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["shootingDayId"]),
        Index(value = ["shootingDayId", "shotSequenceNumber"])
    ]
)
data class CaptureEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val shootingDayId: String,
    val shotNumber: String, // e.g. "SHOT 001"
    val shotSequenceNumber: Int,
    val scene: String = "",
    val take: String = "Take 01",
    val subject: String = "",
    val description: String = "",
    val imagePath: String,
    val thumbnailPath: String? = null,
    val isReference: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Real Hardware Camera readings frozen at capture time (if available)
    val originalCameraIso: String? = null,
    val originalCameraShutter: String? = null,
    val originalCameraAperture: String? = null,
    val originalCameraFocalLength: String? = null,
    val originalCameraWhiteBalance: String? = null,
    val originalCameraResolution: String? = null,
    val originalCameraExposureComp: String? = null,
    val originalCameraLensModel: String? = null,

    // Historical frozen baseline (inherited values at time of capture)
    // Ensures changing project or day defaults in the future will NOT alter this historical capture
    @Embedded(prefix = "base_cam_") val baselineCamera: CameraSettings = CameraSettings(),
    @Embedded(prefix = "base_vfx_") val baselineVfx: VfxSettings = VfxSettings(),

    // Shot-specific overrides (non-null overrides inherited/baseline value)
    @Embedded(prefix = "shot_cam_") val cameraOverrides: CameraOverrides = CameraOverrides(),
    @Embedded(prefix = "shot_vfx_") val vfxOverrides: VfxOverrides = VfxOverrides(),

    // Notes
    val shotNotes: String = "",
    val additionalNotes: String = "",

    // Location
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val heading: Float? = null,
    val gpsAccuracy: Float? = null,
    val locationName: String? = null,

    // VFX specific fields
    val plateType: String = "",
    val environment: String = "",
    val cameraHeight: String = "",
    val cameraDistance: String = "",
    val cameraTilt: String = "",
    val cameraRoll: String = "",
    val cameraPan: String = "",
    val lensDistortionNotes: String = "",
    val trackingMarkers: String = "",
    val lightingNotes: String = "",
    val vfxNotes: String = "",
    val referenceNotes: String = "",
    val bgFgInfo: String = "",
    val subjectDistance: String = ""
)
