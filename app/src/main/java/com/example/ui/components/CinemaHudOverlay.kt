package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CameraSettings
import com.example.data.model.CaptureEntity
import com.example.ui.theme.VfxAmber
import com.example.ui.theme.VfxBlack
import com.example.ui.theme.VfxBorder
import com.example.ui.theme.VfxCyan
import com.example.ui.theme.VfxGreen
import com.example.ui.theme.VfxPanel
import com.example.ui.theme.VfxRed
import com.example.ui.theme.VfxSurface
import com.example.ui.theme.VfxTextMuted
import com.example.ui.theme.VfxTextPrimary
import com.example.ui.theme.VfxTextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CinemaCrosshairGrid(
    showGrid: Boolean = true,
    showCrosshairs: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!showGrid && !showCrosshairs) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val gridColor = Color(0x35FFFFFF)
        val crosshairColor = Color(0x60F59E0B)

        if (showGrid) {
            // Rule of thirds lines
            drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1f)
            drawLine(gridColor, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), strokeWidth = 1f)

            // Safe action box (90%)
            val marginX = w * 0.05f
            val marginY = h * 0.05f
            drawRect(
                color = Color(0x18FFFFFF),
                topLeft = Offset(marginX, marginY),
                size = androidx.compose.ui.geometry.Size(w - 2 * marginX, h - 2 * marginY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )
        }

        if (showCrosshairs) {
            val cx = w / 2f
            val cy = h / 2f
            val arm = 18f
            val gap = 6f

            // Center target crosshairs with gap
            drawLine(crosshairColor, Offset(cx - arm, cy), Offset(cx - gap, cy), strokeWidth = 1.5f)
            drawLine(crosshairColor, Offset(cx + gap, cy), Offset(cx + arm, cy), strokeWidth = 1.5f)
            drawLine(crosshairColor, Offset(cx, cy - arm), Offset(cx, cy - gap), strokeWidth = 1.5f)
            drawLine(crosshairColor, Offset(cx, cy + gap), Offset(cx, cy + arm), strokeWidth = 1.5f)
        }
    }
}

@Composable
fun CinemaHudTopBar(
    cameraSettings: CameraSettings,
    projectName: String,
    dayLabel: String,
    timeString: String,
    batteryPercent: Int = 92,
    storageGbRemaining: String = "48.2 GB",
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Column {
            // Upper row: Project & Day, Battery, Storage, Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = projectName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        ),
                        color = VfxAmber
                    )
                    Text(
                        text = " · $dayLabel",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = VfxTextMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BatteryFull,
                        contentDescription = "Battery",
                        tint = VfxGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "$batteryPercent%",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = VfxTextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = "Storage",
                        tint = VfxCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = storageGbRemaining,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = VfxTextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onInfoClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Day Metadata",
                            tint = VfxAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main telemetry line: Camera · Lens | ISO · Shutter · Aperture | WB · EV
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${cameraSettings.cameraUnitId} · ${cameraSettings.focalLength} (${cameraSettings.cameraModel})",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = VfxTextPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ISO ${cameraSettings.iso}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = VfxAmber
                    )
                    Text(
                        text = cameraSettings.shutterSpeed,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = VfxTextPrimary
                    )
                    Text(
                        text = cameraSettings.aperture,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = VfxCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Second telemetry line: WB · EV · ColorSpace · FrameRate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WB ${cameraSettings.whiteBalance}  EV ${cameraSettings.exposureCompensation}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                    color = VfxTextMuted
                )
                Text(
                    text = "${cameraSettings.frameRate} · ${cameraSettings.colorSpace}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                    color = VfxTextMuted
                )
            }
        }
    }
}

@Composable
fun CaptureConfirmationBanner(
    capture: CaptureEntity,
    onViewDetails: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(VfxSurface.copy(alpha = 0.95f))
            .border(1.dp, VfxAmber, RoundedCornerShape(8.dp))
            .padding(12.dp)
            .testTag("capture_confirmation_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini thumbnail
            val file = File(capture.thumbnailPath ?: capture.imagePath)
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(VfxBlack)
                    .border(1.dp, VfxBorder, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (file.exists()) {
                    AsyncImage(
                        model = file,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, tint = VfxGreen)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = VfxGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SHOT CAPTURED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = VfxGreen
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = capture.shotNumber,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = VfxAmber
                )
                Text(
                    text = SimpleDateFormat("dd MMM yyyy · HH:mm:ss", Locale.US).format(Date(capture.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = VfxTextMuted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Button(
                    onClick = onViewDetails,
                    colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(32.dp).testTag("confirm_view_details")
                ) {
                    Text("VIEW DETAILS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onDone,
                    border = androidx.compose.foundation.BorderStroke(1.dp, VfxBorder),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(28.dp).testTag("confirm_done")
                ) {
                    Text("DONE", style = MaterialTheme.typography.labelSmall, color = VfxTextSecondary)
                }
            }
        }
    }
}
