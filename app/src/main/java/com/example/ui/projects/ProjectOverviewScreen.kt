package com.example.ui.projects

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectEntity
import com.example.data.model.ShootingDayEntity
import com.example.data.repository.VfxRepository
import com.example.ui.components.VfxCard
import com.example.ui.components.VfxTopBar
import com.example.ui.theme.VfxAmber
import com.example.ui.theme.VfxBlack
import com.example.ui.theme.VfxBorder
import com.example.ui.theme.VfxBorderSubtle
import com.example.ui.theme.VfxCyan
import com.example.ui.theme.VfxPanel
import com.example.ui.theme.VfxRed
import com.example.ui.theme.VfxSurface
import com.example.ui.theme.VfxTextMuted
import com.example.ui.theme.VfxTextPrimary
import com.example.ui.theme.VfxTextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectOverviewScreen(
    repository: VfxRepository,
    projectId: String,
    onNavigateBack: () -> Unit,
    onEditProject: (String) -> Unit,
    onDayClick: (String) -> Unit,
    onOpenCameraForDay: (String, String) -> Unit, // projectId, dayId
    onExportPdf: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val project by repository.observeProject(projectId).collectAsState(initial = null)
    val shootingDays by repository.getShootingDays(projectId).collectAsState(initial = emptyList())
    val allCaptures by repository.getCapturesForProject(projectId).collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Overview, 1: Shooting Days, 2: Camera Setup
    var showAddDayDialog by remember { mutableStateOf(false) }
    var dayPendingDelete by remember { mutableStateOf<ShootingDayEntity?>(null) }

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize().background(VfxBlack), contentAlignment = Alignment.Center) {
            Text("Loading project...", color = VfxTextMuted)
        }
        return
    }

    val currentProj = project!!

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(VfxBlack)) {
                VfxTopBar(
                    title = currentProj.name,
                    subtitle = currentProj.client.ifBlank { "VFX Project" },
                    onBack = onNavigateBack,
                    actions = {
                        IconButton(onClick = { onEditProject(currentProj.id) }, modifier = Modifier.testTag("action_edit_project")) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Project", tint = VfxTextSecondary)
                        }
                        IconButton(onClick = { onExportPdf(currentProj.id) }, modifier = Modifier.testTag("action_export_pdf")) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF Report", tint = VfxAmber)
                        }
                    }
                )

                // Project summary header card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(VfxPanel)
                        .border(1.dp, VfxBorder, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = currentProj.name.uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = VfxAmber
                                )
                                Text(
                                    text = listOf(currentProj.client, currentProj.productionCompany).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VfxTextSecondary
                                )
                            }

                            val latestDay = shootingDays.lastOrNull()
                            if (latestDay != null) {
                                Button(
                                    onClick = { onOpenCameraForDay(currentProj.id, latestDay.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.testTag("button_open_camera_latest")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CAMERA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${shootingDays.size} Shooting Days  ·  ${allCaptures.size} Captures",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = VfxTextPrimary
                            )
                            val updateDate = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(currentProj.updatedAt))
                            Text(
                                text = "Last updated: $updateDate",
                                style = MaterialTheme.typography.labelSmall,
                                color = VfxTextMuted
                            )
                        }
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
                                "OVERVIEW",
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
                                "SHOOTING DAYS (${shootingDays.size})",
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
                                "CAMERA SETUP",
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Quick Action: Add Shooting Day
                    item {
                        Button(
                            onClick = { showAddDayDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth().testTag("button_add_shooting_day")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "+ ADD SHOOTING DAY",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // Default Camera Setup Card (Requirement 4)
                    item {
                        val cam = currentProj.cameraDefaults
                        VfxCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = "DEFAULT CAMERA SETUP",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = VfxAmber
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                val camMakeModel = listOf(cam.cameraManufacturer, cam.cameraModel).filter { it.isNotBlank() }.joinToString(" ")
                                val camUnit = if (cam.cameraUnitId.isNotBlank()) " (${cam.cameraUnitId})" else ""
                                val camDisplay = (camMakeModel + camUnit).trim()
                                SetupRow("Camera", camDisplay)

                                val lensMake = listOf(cam.lensManufacturer, cam.lensModel).filter { it.isNotBlank() }.joinToString(" ")
                                val lensDisplay = listOf(cam.focalLength, if (lensMake.isNotBlank()) "($lensMake)" else "").filter { it.isNotBlank() }.joinToString(" ")
                                SetupRow("Lens", lensDisplay)

                                SetupRow("Resolution", cam.resolution)
                                SetupRow("Frame Rate", cam.frameRate)
                                SetupRow("ISO", cam.iso)
                                SetupRow("Shutter", cam.shutterSpeed)
                                SetupRow("Aperture", cam.aperture)
                                SetupRow("White Balance", cam.whiteBalance)
                                SetupRow("Color Space", cam.colorSpace)
                                SetupRow("Gamma / Log", cam.gammaProfile)
                            }
                        }
                    }

                    // Production & VFX Scope
                    item {
                        val vfx = currentProj.vfxDefaults
                        VfxCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = "VFX DEFAULTS",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = VfxCyan
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                SetupRow("Plate Type", vfx.plateType)
                                SetupRow("Environment", vfx.environment)
                                SetupRow("Camera Height", vfx.cameraHeight)
                                SetupRow("Subject Distance", vfx.defaultCameraDistance)
                                SetupRow("Lighting", vfx.lightingNotes)
                                SetupRow("Tracking Notes", vfx.defaultTrackingNotes)
                            }
                        }
                    }

                    // Crew Info Card
                    item {
                        VfxCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = "PRODUCTION TEAM",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = VfxTextSecondary
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                SetupRow("VFX Supervisor", currentProj.vfxSupervisor.ifBlank { "Not specified" })
                                SetupRow("Director", currentProj.director.ifBlank { "Not specified" })
                                SetupRow("VFX Producer", currentProj.vfxProducer.ifBlank { "Not specified" })
                                SetupRow("Camera Operator", currentProj.cameraOperator.ifBlank { "Not specified" })
                            }
                        }
                    }
                }

                1 -> {
                    // Shooting Days list
                    item {
                        Button(
                            onClick = { showAddDayDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth().testTag("tab_button_add_day")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "+ ADD SHOOTING DAY",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    items(shootingDays, key = { it.id }) { day ->
                        val dayCaptures = allCaptures.filter { it.shootingDayId == day.id }
                        ShootingDayCard(
                            day = day,
                            captureCount = dayCaptures.size,
                            onClick = { onDayClick(day.id) },
                            onOpenCamera = { onOpenCameraForDay(currentProj.id, day.id) },
                            onDelete = { dayPendingDelete = day }
                        )
                    }
                }

                2 -> {
                    // Detailed Camera Setup & Specs
                    val cam = currentProj.cameraDefaults
                    item {
                        VfxCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("CAMERA HARDWARE & SENSOR", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = VfxAmber))
                                Spacer(modifier = Modifier.height(8.dp))
                                SetupRow("Manufacturer", cam.cameraManufacturer)
                                SetupRow("Model", cam.cameraModel)
                                SetupRow("Unit ID", cam.cameraUnitId)
                                SetupRow("Sensor Format", cam.sensorFormat)
                                SetupRow("Sensor Physical Size", cam.sensorSize)
                                SetupRow("Recording Format", cam.recordingFormat)
                            }
                        }
                    }
                    item {
                        VfxCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("OPTICS & EXPOSURE BASELINE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = VfxAmber))
                                Spacer(modifier = Modifier.height(8.dp))
                                SetupRow("Lens Manufacturer", cam.lensManufacturer)
                                SetupRow("Lens Model", cam.lensModel)
                                SetupRow("Lens ID", cam.lensId)
                                SetupRow("Focal Length", cam.focalLength)
                                SetupRow("Aperture", cam.aperture)
                                SetupRow("Shutter Speed", cam.shutterSpeed)
                                SetupRow("Base ISO", cam.iso)
                                SetupRow("White Balance", cam.whiteBalance)
                                SetupRow("Color Space", cam.colorSpace)
                                SetupRow("Gamma / Log Curve", cam.gammaProfile)
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Add Shooting Day Dialog
    if (showAddDayDialog) {
        var dayDate by remember { mutableStateOf(SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())) }
        var dayLocation by remember { mutableStateOf(currentProj.location) }
        var dayNotes by remember { mutableStateOf("") }

        val nextDayNum = (shootingDays.maxOfOrNull { it.dayNumber } ?: 0) + 1

        AlertDialog(
            onDismissRequest = { showAddDayDialog = false },
            title = {
                Text(
                    text = "Add Shooting Day $nextDayNum",
                    style = MaterialTheme.typography.titleMedium,
                    color = VfxTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Day $nextDayNum will automatically inherit camera & VFX defaults from '${currentProj.name}'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VfxAmber
                    )
                    com.example.ui.components.VfxTextField(
                        value = dayDate,
                        onValueChange = { dayDate = it },
                        label = "Date"
                    )
                    com.example.ui.components.VfxTextField(
                        value = dayLocation,
                        onValueChange = { dayLocation = it },
                        label = "Location",
                        placeholder = "e.g. Pune, Stage 4"
                    )
                    com.example.ui.components.VfxTextField(
                        value = dayNotes,
                        onValueChange = { dayNotes = it },
                        label = "Day Notes",
                        placeholder = "Scene goals, schedule...",
                        singleLine = false
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val newDay = repository.createShootingDay(
                                projectId = currentProj.id,
                                date = dayDate.trim(),
                                location = dayLocation.trim(),
                                dayNotes = dayNotes.trim()
                            )
                            showAddDayDialog = false
                            onDayClick(newDay.id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.testTag("confirm_create_day")
                ) {
                    Text("CREATE DAY", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDayDialog = false }) {
                    Text("Cancel", color = VfxTextPrimary)
                }
            },
            containerColor = VfxSurface,
            shape = RoundedCornerShape(8.dp)
        )
    }

    // Delete Day Dialog
    if (dayPendingDelete != null) {
        val d = dayPendingDelete!!
        AlertDialog(
            onDismissRequest = { dayPendingDelete = null },
            title = { Text("Delete Day ${d.dayNumber}?", color = VfxTextPrimary) },
            text = { Text("Are you sure you want to delete Day ${d.dayNumber} and all its captures? This cannot be undone.", color = VfxTextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDel = dayPendingDelete
                        dayPendingDelete = null
                        if (toDel != null) {
                            scope.launch { repository.deleteShootingDay(toDel) }
                        }
                    }
                ) {
                    Text("Delete", color = VfxRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { dayPendingDelete = null }) { Text("Cancel", color = VfxTextPrimary) }
            },
            containerColor = VfxSurface,
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun SetupRow(label: String, value: String) {
    val isSpecified = value.isNotBlank() && value.trim() != "—"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = VfxTextMuted)
        Text(
            text = if (isSpecified) value.trim() else "Not specified",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSpecified) FontWeight.Medium else FontWeight.Normal
            ),
            color = if (isSpecified) VfxTextPrimary else VfxTextMuted
        )
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(VfxBorderSubtle))
}

@Composable
private fun ShootingDayCard(
    day: ShootingDayEntity,
    captureCount: Int,
    onClick: () -> Unit,
    onOpenCamera: () -> Unit,
    onDelete: () -> Unit
) {
    VfxCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("day_card_${day.dayNumber}"),
        onClick = onClick
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(VfxAmber)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "DAY ${day.dayNumber}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = VfxBlack
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = day.date,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = VfxTextPrimary
                        )
                        if (day.location.isNotBlank()) {
                            Text(text = day.location, style = MaterialTheme.typography.bodySmall, color = VfxTextSecondary)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenCamera, modifier = Modifier.size(32.dp).testTag("day_open_camera_${day.dayNumber}")) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = VfxAmber, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = VfxTextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (day.dayNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = day.dayNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = VfxTextMuted,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$captureCount Captures",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = VfxCyan
                )

                if (day.cameraOverrides.hasAnyOverride()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF2B2212))
                            .border(1.dp, Color(0xFF65481B), RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CUSTOM DAY OVERRIDES",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = VfxAmber
                        )
                    }
                } else {
                    Text(
                        text = "Inherits all project defaults",
                        style = MaterialTheme.typography.labelSmall,
                        color = VfxTextMuted
                    )
                }
            }
        }
    }
}
