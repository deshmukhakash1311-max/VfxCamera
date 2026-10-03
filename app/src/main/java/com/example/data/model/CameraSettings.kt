package com.example.data.model

import androidx.room.ColumnInfo

data class CameraSettings(
    @ColumnInfo(name = "cam_manufacturer") val cameraManufacturer: String = "Sony",
    @ColumnInfo(name = "cam_model") val cameraModel: String = "FX6",
    @ColumnInfo(name = "cam_unit_id") val cameraUnitId: String = "A-CAM",
    @ColumnInfo(name = "lens_manufacturer") val lensManufacturer: String = "Zeiss",
    @ColumnInfo(name = "lens_model") val lensModel: String = "Supreme Prime",
    @ColumnInfo(name = "lens_id") val lensId: String = "L-01",
    @ColumnInfo(name = "focal_length") val focalLength: String = "24mm",
    @ColumnInfo(name = "sensor_format") val sensorFormat: String = "Full Frame 35mm",
    @ColumnInfo(name = "sensor_size") val sensorSize: String = "35.7 x 18.8 mm",
    @ColumnInfo(name = "resolution") val resolution: String = "4096x2160 (4K DCI)",
    @ColumnInfo(name = "frame_rate") val frameRate: String = "24 fps",
    @ColumnInfo(name = "iso") val iso: String = "800",
    @ColumnInfo(name = "shutter_speed") val shutterSpeed: String = "1/48",
    @ColumnInfo(name = "aperture") val aperture: String = "f/2.8",
    @ColumnInfo(name = "white_balance") val whiteBalance: String = "5600K",
    @ColumnInfo(name = "exposure_comp") val exposureCompensation: String = "0.0 EV",
    @ColumnInfo(name = "color_space") val colorSpace: String = "S-Gamut3.Cine",
    @ColumnInfo(name = "gamma_profile") val gammaProfile: String = "S-Log3",
    @ColumnInfo(name = "recording_format") val recordingFormat: String = "XAVC-I 422 10-bit"
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
