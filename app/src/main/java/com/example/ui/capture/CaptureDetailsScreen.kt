package com.example.ui.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CameraOverrides
import com.example.data.model.CaptureEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ShootingDayEntity
import com.example.data.model.VfxOverrides
import com.example.data.repository.VfxRepository
import com.example.domain.inheritance.MetadataResolver
import com.example.ui.components.MetadataDisplayRow
import com.example.ui.components.VfxCard
import com.example.ui.components.VfxTextField
import com.example.ui.components.VfxTopBar
import com.example.ui.theme.VfxAmber
import com.example.ui.theme.VfxBlack
import com.example.ui.theme.VfxBorder
import com.example.ui.theme.VfxCyan
import com.example.ui.theme.VfxRed
import com.example.ui.theme.VfxSurface
import com.example.ui.theme.VfxTextMuted
import com.example.ui.theme.VfxTextPrimary
import com.example.ui.theme.VfxTextSecondary
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CaptureDetailsScreen(
    repository: VfxRepository,
    captureId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val capture by repository.observeCapture(captureId).collectAsState(initial = null)

    var selectedTab by remember { mutableIntStateOf(0) } // 0: DETAILS, 1: NOTES, 2: LOCATION, 3: VFX
    var fieldEditingKey by remember { mutableStateOf<String?>(null) }
    var fieldEditingLabel by remember { mutableStateOf("") }
    var fieldEditingValue by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (capture == null) {
        Box(modifier = Modifier.fillMaxSize().background(VfxBlack), contentAlignment = Alignment.Center) {
            Text("Loading Shot Details...", color = VfxTextMuted)
        }
        return
    }

    val currentCapture = capture!!
    val project by repository.observeProject(currentCapture.projectId).collectAsState(initial = null)
    val day by repository.observeShootingDay(currentCapture.shootingDayId).collectAsState(initial = null)

    val cameraFields = MetadataResolver.inspectCaptureCameraFields(currentCapture, day, project)
    val vfxFields = MetadataResolver.inspectCaptureVfxFields(currentCapture, day, project)

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(VfxBlack)) {
                VfxTopBar(
                    title = currentCapture.shotNumber,
                    subtitle = "Day ${day?.dayNumber ?: 1} · ${currentCapture.scene.ifBlank { "Scene" }}",
                    onBack = onNavigateBack,
                    actions = {
                        IconButton(
                            onClick = {
                                scope.launch { repository.setReference(currentCapture.id, !currentCapture.isReference) }
                            },
                            modifier = Modifier.testTag("action_toggle_reference")
                        ) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = "Reference",
                                tint = if (currentCapture.isReference) VfxAmber else VfxTextMuted
                            )
                        }
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("action_delete_shot")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = VfxRed)
                        }
                    }
                )

                // Shot Header strip with large image preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(VfxSurface)
                        .border(1.dp, VfxBorder, RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val file = File(currentCapture.imagePath)
                    Box(
                        modifier = Modifier
                            .size(width = 84.dp, height = 64.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(VfxBlack)
                            .border(1.dp, VfxBorder, RoundedCornerShape(4.dp))
                    ) {
                        if (file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = currentCapture.shotNumber,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentCapture.shotNumber,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = VfxAmber
                            )
                            if (currentCapture.isReference) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "★ HERO REFERENCE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = VfxAmber
                                )
                            }
                        }
                        Text(
                            text = "${currentCapture.scene.ifBlank { "Scene" }} · ${currentCapture.take}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VfxTextPrimary
                        )
                        Text(
                            text = SimpleDateFormat("dd MMM yyyy · HH:mm:ss", Locale.US).format(Date(currentCapture.createdAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = VfxTextMuted
                        )
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = VfxBlack,
                    contentColor = VfxAmber,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = VfxAmber,
                            height = 2.dp
                        )
                    }
                ) {
                    listOf("DETAILS", "NOTES", "LOCATION", "VFX").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = if (selectedTab == index) VfxAmber else VfxTextMuted
                                )
                            }
                        )
                    }
                }
            }
        },
        containerColor = VfxBlack,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Shot Information Card
                    item {
                        var sceneText by remember { mutableStateOf(currentCapture.scene) }
                        var takeText by remember { mutableStateOf(currentCapture.take) }
                        var subjectText by remember { mutableStateOf(currentCapture.subject) }
                        var descText by remember { mutableStateOf(currentCapture.description) }

                        VfxCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("SHOT INFORMATION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = VfxAmber))
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    VfxTextField(value = sceneText, onValueChange = { sceneText = it }, label = "Scene", placeholder = "PC Screen", modifier = Modifier.weight(1f))
                                    VfxTextField(value = takeText, onValueChange = { takeText = it }, label = "Take", placeholder = "Take 01", modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                VfxTextField(value = subjectText, onValueChange = { subjectText = it }, label = "Subject", placeholder = "Main actor monitor")
                                Spacer(modifier = Modifier.height(8.dp))
                                VfxTextField(value = descText, onValueChange = { descText = it }, label = "Description", placeholder = "Shot description...", singleLine = false)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            repository.saveCapture(
                                                currentCapture.copy(
                                                    scene = sceneText.trim(),
                                                    take = takeText.trim(),
                                                    subject = subjectText.trim(),
                                                    description = descText.trim()
                                                )
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("save_shot_info_button")
                                ) {
                                    Text("SAVE SHOT INFO", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Provenance explanation banner
                    item {
                        Text(
                            text = "CAMERA METADATA & INHERITANCE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = VfxAmber
                        )
                        Text(
                            text = "Values reflect baseline inherited at capture time. Tap any row to set a Shot Override. Original sensor values are preserved for traceability.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VfxTextSecondary
                        )
                    }

                    // Camera Metadata items
                    items(cameraFields, key = { it.key }) { field ->
                        VfxCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("capture_meta_${field.key}"),
                            onClick = {
                                fieldEditingKey = field.key
                                fieldEditingLabel = field.label
                                fieldEditingValue = field.value
                            }
                        ) {
                            MetadataDisplayRow(
                                label = field.label,
                                value = field.value,
                                source = field.source,
                                originalValue = field.originalCameraValue,
                                onReset = if (field.isOverridden) {
                                    {
                                        scope.launch {
                                            val newOverrides = updateCaptureCameraOverride(currentCapture.cameraOverrides, field.key, null)
                                            repository.saveCapture(currentCapture.copy(cameraOverrides = newOverrides))
                                        }
                                    }
                                } else null
                            )
                        }
                    }
                }

                1 -> {
                    // Notes Tab (Requirement 15)
                    item {
                        var shotNotesText by remember { mutableStateOf(currentCapture.shotNotes) }
                        var additionalNotesText by remember { mutableStateOf(currentCapture.additionalNotes) }

                        VfxCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("SHOT NOTES", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = VfxAmber))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("E.g. Screen replacement reference. Monitor visible in frame. Avoid reflections.", style = MaterialTheme.typography.bodySmall, color = VfxTextMuted)
                                Spacer(modifier = Modifier.height(10.dp))
                                VfxTextField(
                                    value = shotNotesText,
                                    onValueChange = { shotNotesText = it },
                                    label = "Shot Notes",
                                    placeholder = "Enter free-form shot notes...",
                                    singleLine = false
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                VfxTextField(
                                    value = additionalNotesText,
                                    onValueChange = { additionalNotesText = it },
                                    label = "Additional Notes",
                                    placeholder = "Extra technical or post-production notes...",
                                    singleLine = false
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            repository.saveCapture(
                                                currentCapture.copy(
                                                    shotNotes = shotNotesText.trim(),
                                                    additionalNotes = additionalNotesText.trim()
                                                )
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("save_notes_button")
                                ) {
                                    Text("SAVE NOTES", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Location Tab (Requirement 16)
                    item {
                        var latText by remember { mutableStateOf(currentCapture.latitude?.toString() ?: "") }
                        var lonText by remember { mutableStateOf(currentCapture.longitude?.toString() ?: "") }
                        var altText by remember { mutableStateOf(currentCapture.altitude?.toString() ?: "") }
                        var locNameText by remember { mutableStateOf(currentCapture.locationName ?: "") }

                        VfxCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("LOCATION & GEODATA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = VfxAmber))
                                Spacer(modifier = Modifier.height(10.dp))
                                VfxTextField(value = locNameText, onValueChange = { locNameText = it }, label = "Location Name", placeholder = "Stage 4, Pinewood Studios")
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    VfxTextField(value = latText, onValueChange = { latText = it }, label = "Latitude", placeholder = "18.5204", modifier = Modifier.weight(1f))
                                    VfxTextField(value = lonText, onValueChange = { lonText = it }, label = "Longitude", placeholder = "73.8567", modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                VfxTextField(value = altText, onValueChange = { altText = it }, label = "Altitude (m)", placeholder = "560 m")
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            repository.saveCapture(
                                                currentCapture.copy(
                                                    latitude = latText.toDoubleOrNull(),
                                                    longitude = lonText.toDoubleOrNull(),
                                                    altitude = altText.toDoubleOrNull(),
                                                    locationName = locNameText.trim()
                                                )
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("SAVE LOCATION", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // VFX Tab (Requirement 17)
                    item {
                        var plateTypeText by remember { mutableStateOf(currentCapture.plateType.ifBlank { currentCapture.baselineVfx.plateType }) }
                        var envText by remember { mutableStateOf(currentCapture.environment.ifBlank { currentCapture.baselineVfx.environment }) }
                        var camHeightText by remember { mutableStateOf(currentCapture.cameraHeight.ifBlank { currentCapture.baselineVfx.cameraHeight }) }
                        var camDistText by remember { mutableStateOf(currentCapture.cameraDistance.ifBlank { currentCapture.baselineVfx.defaultCameraDistance }) }
                        var camTiltText by remember { mutableStateOf(currentCapture.cameraTilt) }
                        var camRollText by remember { mutableStateOf(currentCapture.cameraRoll) }
                        var camPanText by remember { mutableStateOf(currentCapture.cameraPan) }
                        var lensDistortText by remember { mutableStateOf(currentCapture.lensDistortionNotes) }
                        var trackingMarkersText by remember { mutableStateOf(currentCapture.trackingMarkers.ifBlank { currentCapture.baselineVfx.defaultTrackingNotes }) }
                        var lightingNotesText by remember { mutableStateOf(currentCapture.lightingNotes.ifBlank { currentCapture.baselineVfx.lightingNotes }) }
                        var vfxNotesText by remember { mutableStateOf(currentCapture.vfxNotes.ifBlank { currentCapture.baselineVfx.generalVfxNotes }) }
                        var refNotesText by remember { mutableStateOf(currentCapture.referenceNotes) }
                        var bgFgInfoText by remember { mutableStateOf(currentCapture.bgFgInfo) }
                        var subjectDistText by remember { mutableStateOf(currentCapture.subjectDistance) }

                        VfxCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("VFX & CAMERA TRACKING DATA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = VfxCyan))
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    VfxTextField(value = plateTypeText, onValueChange = { plateTypeText = it }, label = "Plate Type", placeholder = "Reference / Monitor Pass", modifier = Modifier.weight(1f))
                                    VfxTextField(value = envText, onValueChange = { envText = it }, label = "Environment", placeholder = "Interior Stage", modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    VfxTextField(value = camHeightText, onValueChange = { camHeightText = it }, label = "Camera Height", placeholder = "1.5 m", modifier = Modifier.weight(1f))
                                    VfxTextField(value = camDistText, onValueChange = { camDistText = it }, label = "Camera Distance", placeholder = "2.8 m", modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    VfxTextField(value = camTiltText, onValueChange = { camTiltText = it }, label = "Tilt", placeholder = "-5 deg", modifier = Modifier.weight(1f))
                                    VfxTextField(value = camRollText, onValueChange = { camRollText = it }, label = "Roll", placeholder = "0 deg", modifier = Modifier.weight(1f))
                                    VfxTextField(value = camPanText, onValueChange = { camPanText = it }, label = "Pan / Heading", placeholder = "45 deg", modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                VfxTextField(value = trackingMarkersText, onValueChange = { trackingMarkersText = it }, label = "Tracking Markers", placeholder = "Orange X markers placed 30cm apart")
                                Spacer(modifier = Modifier.height(8.dp))
                                VfxTextField(value = lightingNotesText, onValueChange = { lightingNotesText = it }, label = "Lighting Notes", placeholder = "Key 5600K 45deg, ambient fill")
                                Spacer(modifier = Modifier.height(8.dp))
                                VfxTextField(value = lensDistortText, onValueChange = { lensDistortText = it }, label = "Lens Distortion Notes", placeholder = "Grid chart shot captured at end of day")
                                Spacer(modifier = Modifier.height(8.dp))
                                VfxTextField(value = vfxNotesText, onValueChange = { vfxNotesText = it }, label = "VFX Notes", placeholder = "Screen replacement reference notes...", singleLine = false)
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            repository.saveCapture(
                                                currentCapture.copy(
                                                    plateType = plateTypeText.trim(),
                                                    environment = envText.trim(),
                                                    cameraHeight = camHeightText.trim(),
                                                    cameraDistance = camDistText.trim(),
                                                    cameraTilt = camTiltText.trim(),
                                                    cameraRoll = camRollText.trim(),
                                                    cameraPan = camPanText.trim(),
                                                    lensDistortionNotes = lensDistortText.trim(),
                                                    trackingMarkers = trackingMarkersText.trim(),
                                                    lightingNotes = lightingNotesText.trim(),
                                                    vfxNotes = vfxNotesText.trim(),
                                                    referenceNotes = refNotesText.trim(),
                                                    bgFgInfo = bgFgInfoText.trim(),
                                                    subjectDistance = subjectDistText.trim()
                                                )
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("save_vfx_button")
                                ) {
                                    Text("SAVE VFX DATA", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Edit Field Dialog for Shot Override (Requirement 13, 14)
    if (fieldEditingKey != null) {
        val key = fieldEditingKey!!
        AlertDialog(
            onDismissRequest = { fieldEditingKey = null },
            title = {
                Text(
                    text = "Edit $fieldEditingLabel",
                    color = VfxTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    Text(
                        text = "Setting a value here creates a Shot Override for ${currentCapture.shotNumber}. Day and Project values remain untouched.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VfxAmber
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    VfxTextField(
                        value = fieldEditingValue,
                        onValueChange = { fieldEditingValue = it },
                        label = fieldEditingLabel
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val newOverrides = updateCaptureCameraOverride(currentCapture.cameraOverrides, key, fieldEditingValue.trim())
                            repository.saveCapture(currentCapture.copy(cameraOverrides = newOverrides))
                            fieldEditingKey = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.testTag("dialog_save_shot_override")
                ) {
                    Text("SET SHOT OVERRIDE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val newOverrides = updateCaptureCameraOverride(currentCapture.cameraOverrides, key, null)
                                repository.saveCapture(currentCapture.copy(cameraOverrides = newOverrides))
                                fieldEditingKey = null
                            }
                        }
                    ) {
                        Text("Reset to Inherited", color = VfxTextMuted)
                    }
                    TextButton(onClick = { fieldEditingKey = null }) {
                        Text("Cancel", color = VfxTextPrimary)
                    }
                }
            },
            containerColor = VfxSurface,
            shape = RoundedCornerShape(8.dp)
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${currentCapture.shotNumber}?", color = VfxTextPrimary) },
            text = { Text("Are you sure you want to delete this shot and its metadata? This cannot be undone.", color = VfxTextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            repository.deleteCapture(currentCapture)
                            onNavigateBack()
                        }
                    }
                ) {
                    Text("Delete", color = VfxRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = VfxTextPrimary)
                }
            },
            containerColor = VfxSurface,
            shape = RoundedCornerShape(8.dp)
        )
    }
}

private fun updateCaptureCameraOverride(existing: CameraOverrides, key: String, value: String?): CameraOverrides {
    return when (key) {
        "cameraManufacturer" -> existing.copy(cameraManufacturer = value)
        "cameraModel" -> existing.copy(cameraModel = value)
        "cameraUnitId" -> existing.copy(cameraUnitId = value)
        "lensManufacturer" -> existing.copy(lensManufacturer = value)
        "lensModel" -> existing.copy(lensModel = value)
        "lensId" -> existing.copy(lensId = value)
        "focalLength" -> existing.copy(focalLength = value)
        "sensorFormat" -> existing.copy(sensorFormat = value)
        "sensorSize" -> existing.copy(sensorSize = value)
        "resolution" -> existing.copy(resolution = value)
        "frameRate" -> existing.copy(frameRate = value)
        "iso" -> existing.copy(iso = value)
        "shutterSpeed" -> existing.copy(shutterSpeed = value)
        "aperture" -> existing.copy(aperture = value)
        "whiteBalance" -> existing.copy(whiteBalance = value)
        "exposureCompensation" -> existing.copy(exposureCompensation = value)
        "colorSpace" -> existing.copy(colorSpace = value)
        "gammaProfile" -> existing.copy(gammaProfile = value)
        "recordingFormat" -> existing.copy(recordingFormat = value)
        else -> existing
    }
}
