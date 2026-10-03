package com.example.data.model

import androidx.room.ColumnInfo

data class CameraSettings(
    @ColumnInfo(name = "cam_manufacturer") val cameraManufacturer: String = "",
    @ColumnInfo(name = "cam_model") val cameraModel: String = "",
    @ColumnInfo(name = "cam_unit_id") val cameraUnitId: String = "",
    @ColumnInfo(name = "lens_manufacturer") val lensManufacturer: String = "",
    @ColumnInfo(name = "lens_model") val lensModel: String = "",
    @ColumnInfo(name = "lens_id") val lensId: String = "",
    @ColumnInfo(name = "focal_length") val focalLength: String = "",
    @ColumnInfo(name = "sensor_format") val sensorFormat: String = "",
    @ColumnInfo(name = "sensor_size") val sensorSize: String = "",
    @ColumnInfo(name = "resolution") val resolution: String = "",
    @ColumnInfo(name = "frame_rate") val frameRate: String = "",
    @ColumnInfo(name = "iso") val iso: String = "",
    @ColumnInfo(name = "shutter_speed") val shutterSpeed: String = "",
    @ColumnInfo(name = "aperture") val aperture: String = "",
    @ColumnInfo(name = "white_balance") val whiteBalance: String = "",
    @ColumnInfo(name = "exposure_comp") val exposureCompensation: String = "",
    @ColumnInfo(name = "color_space") val colorSpace: String = "",
    @ColumnInfo(name = "gamma_profile") val gammaProfile: String = "",
    @ColumnInfo(name = "recording_format") val recordingFormat: String = ""
)

data class CameraOverrides(
    val cameraManufacturer: String? = null,
    val cameraModel: String? = null,
    val cameraUnitId: String? = null,
    val lensManufacturer: String? = null,
    val lensModel: String? = null,
    val lensId: String? = null,
    val focalLength: String? = null,
    val sensorFormat: String? = null,
    val sensorSize: String? = null,
    val resolution: String? = null,
    val frameRate: String? = null,
    val iso: String? = null,
    val shutterSpeed: String? = null,
    val aperture: String? = null,
    val whiteBalance: String? = null,
    val exposureCompensation: String? = null,
    val colorSpace: String? = null,
    val gammaProfile: String? = null,
    val recordingFormat: String? = null
) {
    fun hasAnyOverride(): Boolean =
        cameraManufacturer != null || cameraModel != null || cameraUnitId != null ||
        lensManufacturer != null || lensModel != null || lensId != null ||
        focalLength != null || sensorFormat != null || sensorSize != null ||
        resolution != null || frameRate != null || iso != null ||
        shutterSpeed != null || aperture != null || whiteBalance != null ||
        exposureCompensation != null || colorSpace != null || gammaProfile != null ||
        recordingFormat != null
}
