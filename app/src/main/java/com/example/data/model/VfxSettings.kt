package com.example.data.model

import androidx.room.ColumnInfo

data class VfxSettings(
    @ColumnInfo(name = "vfx_plate_type") val plateType: String = "",
    @ColumnInfo(name = "vfx_environment") val environment: String = "",
    @ColumnInfo(name = "vfx_lighting_notes") val lightingNotes: String = "",
    @ColumnInfo(name = "vfx_general_notes") val generalVfxNotes: String = "",
    @ColumnInfo(name = "vfx_camera_height") val cameraHeight: String = "",
    @ColumnInfo(name = "vfx_camera_distance") val defaultCameraDistance: String = "",
    @ColumnInfo(name = "vfx_tracking_notes") val defaultTrackingNotes: String = ""
)

data class VfxOverrides(
    val plateType: String? = null,
    val environment: String? = null,
    val lightingNotes: String? = null,
    val generalVfxNotes: String? = null,
    val cameraHeight: String? = null,
    val defaultCameraDistance: String? = null,
    val defaultTrackingNotes: String? = null
) {
    fun hasAnyOverride(): Boolean =
        plateType != null || environment != null || lightingNotes != null ||
        generalVfxNotes != null || cameraHeight != null || defaultCameraDistance != null ||
        defaultTrackingNotes != null
}
