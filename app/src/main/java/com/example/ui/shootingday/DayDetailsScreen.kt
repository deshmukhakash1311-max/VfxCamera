package com.example.ui.shootingday

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CameraOverrides
import com.example.data.model.CaptureEntity
import com.example.data.model.InheritanceSource
import com.example.data.model.ProjectEntity
import com.example.data.model.ShootingDayEntity
import com.example.data.repository.VfxRepository
import com.example.domain.inheritance.MetadataResolver
import com.example.ui.components.MetadataDisplayRow
import com.example.ui.components.ThumbnailStrip
import com.example.ui.components.VfxCard
import com.example.ui.components.VfxTextField
import com.example.ui.components.VfxTopBar
import com.example.ui.theme.VfxAmber
import com.example.ui.theme.VfxBlack
import com.example.ui.theme.VfxBorder
import com.example.ui.theme.VfxSurface
import com.example.ui.theme.VfxTextMuted
import com.example.ui.theme.VfxTextPrimary
import com.example.ui.theme.VfxTextSecondary
import kotlinx.coroutines.launch

@Composable
fun DayDetailsScreen(
    repository: VfxRepository,
    projectId: String,
    dayId: String,
    onNavigateBack: () -> Unit,
    onOpenCamera: () -> Unit,
    onCaptureClick: (String) -> Unit,
    onOpenShotList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val project by repository.observeProject(projectId).collectAsState(initial = null)
    val day by repository.observeShootingDay(dayId).collectAsState(initial = null)
    val captures by repository.getCapturesForDay(dayId).collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) } // 0: CAMERA METADATA, 1: CAPTURES, 2: DAY INFO
    var fieldEditingKey by remember { mutableStateOf<String?>(null) }
    var fieldEditingLabel by remember { mutableStateOf("") }
    var fieldEditingValue by remember { mutableStateOf("") }

    if (day == null || project == null) {
        Box(modifier = Modifier.fillMaxSize().background(VfxBlack), contentAlignment = Alignment.Center) {
            Text("Loading Day Details...", color = VfxTextMuted)
        }
        return
    }

    val currentDay = day!!
    val currentProj = project!!

    val resolvedFields = MetadataResolver.inspectDayCameraFields(currentProj, currentDay)

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(VfxBlack)) {
                VfxTopBar(
                    title = "DAY ${currentDay.dayNumber} DETAILS",
                    subtitle = "${currentProj.name} · ${currentDay.date}",
                    onBack = onNavigateBack,
                    actions = {
                        IconButton(onClick = onOpenShotList, modifier = Modifier.testTag("action_shot_list")) {
                            Icon(Icons.Default.List, contentDescription = "Shot List", tint = VfxTextSecondary)
                        }
                    }
                )

                // Action header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DAY ${currentDay.dayNumber} — ${currentDay.date}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = VfxAmber
                        )
                        Text(
                            text = currentDay.location.ifBlank { "Location not set" },
                            style = MaterialTheme.typography.bodySmall,
                            color = VfxTextSecondary
                        )
                    }

                    Button(
                        onClick = onOpenCamera,
                        colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.testTag("button_open_camera_from_day")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("OPEN CAMERA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
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
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "CAMERA METADATA",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (selectedTab == 0) VfxAmber else VfxTextMuted
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "CAPTURES (${captures.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (selectedTab == 1) VfxAmber else VfxTextMuted
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "DAY NOTES",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (selectedTab == 2) VfxAmber else VfxTextMuted
                            )
                        }
                    )
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
                    item {
                        Text(
                            text = "METADATA INHERITANCE SYSTEM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = VfxTextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Values marked 'INHERITED (PROJECT)' come from Project defaults. Tapping any field allows setting a Day Override. Overrides automatically propagate to all new captures under Day ${currentDay.dayNumber}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VfxTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(resolvedFields, key = { it.key }) { field ->
                        VfxCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("meta_row_${field.key}"),
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
                                onReset = if (field.isOverridden) {
                                    {
                                        scope.launch {
                                            val newOverrides = updateDayOverrideKey(currentDay.cameraOverrides, field.key, null)
                                            repository.saveShootingDay(currentDay.copy(cameraOverrides = newOverrides))
                                        }
                                    }
                                } else null
                            )
                        }
                    }
                }

                1 -> {
                    item {
                        ThumbnailStrip(
                            captures = captures,
                            selectedCaptureId = null,
                            onCaptureClick = { onCaptureClick(it.id) },
                            onEditClick = { onCaptureClick(it.id) },
                            onDeleteClick = { capture ->
                                scope.launch { repository.deleteCapture(capture) }
                            },
                            onToggleReference = { capture ->
                                scope.launch { repository.setReference(capture.id, !capture.isReference) }
                            },
                            onNewShotClick = onOpenCamera
                        )
                    }

                    if (captures.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = VfxBorder, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("No captures yet for Day ${currentDay.dayNumber}", style = MaterialTheme.typography.bodyMedium, color = VfxTextSecondary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = onOpenCamera,
                                        colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("START CAPTURING", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CAPTURED SHOTS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = VfxAmber
                                )
                                OutlinedButton(
                                    onClick = onOpenShotList,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, VfxBorder),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("VIEW FULL LIST", style = MaterialTheme.typography.labelSmall, color = VfxTextPrimary)
                                }
                            }
                        }

                        items(captures, key = { it.id }) { capture ->
                            VfxCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onCaptureClick(capture.id) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = capture.shotNumber,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = VfxAmber
                                        )
                                        Text(
                                            text = listOf(capture.scene, capture.take).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Scene Take" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VfxTextPrimary
                                        )
                                    }
                                    val effIso = capture.cameraOverrides.iso ?: capture.baselineCamera.iso
                                    val effLens = capture.cameraOverrides.focalLength ?: capture.baselineCamera.focalLength
                                    Text(
                                        text = "$effLens · ISO $effIso",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = VfxTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        var dayLocationText by remember { mutableStateOf(currentDay.location) }
                        var dayNotesText by remember { mutableStateOf(currentDay.dayNotes) }

                        VfxCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("EDIT DAY INFORMATION", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = VfxAmber))
                                Spacer(modifier = Modifier.height(10.dp))
                                VfxTextField(
                                    value = dayLocationText,
                                    onValueChange = { dayLocationText = it },
                                    label = "Shooting Location",
                                    placeholder = "e.g. Pune, Stage 4"
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                VfxTextField(
                                    value = dayNotesText,
                                    onValueChange = { dayNotesText = it },
                                    label = "Day Notes",
                                    placeholder = "Notes for this shooting day...",
                                    singleLine = false
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            repository.saveShootingDay(
                                                currentDay.copy(
                                                    location = dayLocationText.trim(),
                                                    dayNotes = dayNotesText.trim()
                                                )
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("SAVE DAY INFO", fontWeight = FontWeight.Bold)
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

    // Edit Field Dialog for Day Override
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
                        text = "Setting a value here creates a Day Override for Day ${currentDay.dayNumber}. Project default remains unchanged.",
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
                            val newOverrides = updateDayOverrideKey(currentDay.cameraOverrides, key, fieldEditingValue.trim())
                            repository.saveShootingDay(currentDay.copy(cameraOverrides = newOverrides))
                            fieldEditingKey = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.testTag("dialog_save_override")
                ) {
                    Text("SET OVERRIDE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val newOverrides = updateDayOverrideKey(currentDay.cameraOverrides, key, null)
                                repository.saveShootingDay(currentDay.copy(cameraOverrides = newOverrides))
                                fieldEditingKey = null
                            }
                        }
                    ) {
                        Text("Reset to Default", color = VfxTextMuted)
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
}

private fun updateDayOverrideKey(existing: CameraOverrides, key: String, value: String?): CameraOverrides {
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
