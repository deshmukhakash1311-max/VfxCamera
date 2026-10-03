package com.example.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.util.Locale

data class DeviceCameraReading(
    val iso: String? = null,
    val shutterSpeed: String? = null,
    val aperture: String? = null,
    val focalLength: String? = null,
    val whiteBalance: String? = null,
    val resolution: String? = null,
    val exposureCompensation: String? = null,
    val lensModel: String? = null
)

object CameraMetadataReader {

    /**
     * Reads actual hardware capture metadata from JPEG image bytes using ExifInterface.
     * Follows Rule 35: Never invent fake values. If not present in EXIF, returns null so
     * UI clearly shows "Not available" or allows manual entry.
     */
    fun extractExifMetadata(jpegBytes: ByteArray): DeviceCameraReading {
        return try {
            val exifInterface = ExifInterface(ByteArrayInputStream(jpegBytes))

            val iso = exifInterface.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                ?.takeIf { it.isNotBlank() }

            val exposureTimeVal = exifInterface.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
            val shutterSpeed = if (exposureTimeVal > 0) {
                if (exposureTimeVal < 1.0) {
                    val denom = (1.0 / exposureTimeVal).toInt()
                    "1/$denom"
                } else {
                    "%.1fs".format(Locale.US, exposureTimeVal)
                }
            } else null

            val fNumber = exifInterface.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
            val aperture = if (fNumber > 0) "f/%.1f".format(Locale.US, fNumber) else null

            val focalLengthVal = exifInterface.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
            val focalLength = if (focalLengthVal > 0) "%.1fmm".format(Locale.US, focalLengthVal) else null

            val wbAttr = exifInterface.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1)
            val whiteBalance = when (wbAttr) {
                ExifInterface.WHITEBALANCE_AUTO -> "Auto WB"
                ExifInterface.WHITEBALANCE_MANUAL -> "Manual WB"
                else -> null
            }

            val width = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            val length = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
            val resolution = if (width > 0 && length > 0) "${width}x$length" else null

            val make = exifInterface.getAttribute(ExifInterface.TAG_MAKE)
            val model = exifInterface.getAttribute(ExifInterface.TAG_MODEL)
            val lensModel = if (!model.isNullOrBlank()) {
                if (!make.isNullOrBlank() && !model.contains(make, ignoreCase = true)) {
                    "$make $model"
                } else model
            } else null

            DeviceCameraReading(
                iso = iso,
                shutterSpeed = shutterSpeed,
                aperture = aperture,
                focalLength = focalLength,
                whiteBalance = whiteBalance,
                resolution = resolution,
                exposureCompensation = null,
                lensModel = lensModel
            )
        } catch (_: Exception) {
            DeviceCameraReading()
        }
    }

    /**
     * Inspect device camera hardware capabilities from Camera2 API.
     */
    fun getDeviceCameraSpecs(context: Context): List<String> {
        val specs = mutableListOf<String>()
        try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return specs
            for (id in manager.cameraIdList) {
                val chars = manager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                val facingStr = when (facing) {
                    CameraCharacteristics.LENS_FACING_BACK -> "Back Camera (Unit $id)"
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front Camera (Unit $id)"
                    else -> "External Camera (Unit $id)"
                }
                val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val fLengthStr = focalLengths?.joinToString(", ") { "%.1fmm".format(Locale.US, it) } ?: "Not available"

                val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                val apertureStr = apertures?.joinToString(", ") { "f/%.1f".format(Locale.US, it) } ?: "Not available"

                val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                val sensorStr = if (sensorSize != null) "%.2fx%.2f mm".format(Locale.US, sensorSize.width, sensorSize.height) else "Not available"

                specs.add("$facingStr: Lens $fLengthStr, Aperture $apertureStr, Sensor $sensorStr")
            }
        } catch (_: Exception) {
            // Camera specs not accessible in this context
        }
        return specs
    }
}
