package com.example.ui.projects

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CameraSettings
import com.example.data.model.ProjectEntity
import com.example.data.model.VfxSettings
import com.example.data.repository.VfxRepository
import com.example.ui.components.VfxTextField
import com.example.ui.components.VfxTopBar
import com.example.ui.theme.VfxAmber
import com.example.ui.theme.VfxBlack
import com.example.ui.theme.VfxBorder
import com.example.ui.theme.VfxTextMuted
import com.example.ui.theme.VfxTextPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CreateEditProjectScreen(
    repository: VfxRepository,
    projectId: String?,
    onNavigateBack: () -> Unit,
    onProjectSaved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedSection by remember { mutableIntStateOf(0) } // 0: Project, 1: Camera Defaults, 2: VFX Defaults

    var existingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var client by remember { mutableStateOf("") }
    var productionCompany by remember { mutableStateOf("") }
    var productionShow by remember { mutableStateOf("") }
    var projectIdCode by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var director by remember { mutableStateOf("") }
    var vfxSupervisor by remember { mutableStateOf("") }
    var vfxProducer by remember { mutableStateOf("") }
    var cameraOperator by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())) }
    var location by remember { mutableStateOf("") }

    // Camera Defaults
    var camManufacturer by remember { mutableStateOf("Sony") }
    var camModel by remember { mutableStateOf("FX6") }
    var camUnitId by remember { mutableStateOf("A-CAM") }
    var lensManufacturer by remember { mutableStateOf("Zeiss") }
    var lensModel by remember { mutableStateOf("Supreme Prime") }
    var lensId by remember { mutableStateOf("SP-24") }
    var focalLength by remember { mutableStateOf("24mm") }
    var sensorFormat by remember { mutableStateOf("Full Frame 35mm") }
    var sensorSize by remember { mutableStateOf("35.7 x 18.8 mm") }
    var resolution by remember { mutableStateOf("4096x2160 (4K DCI)") }
    var frameRate by remember { mutableStateOf("24 fps") }
    var iso by remember { mutableStateOf("800") }
    var shutterSpeed by remember { mutableStateOf("1/48") }
    var aperture by remember { mutableStateOf("f/2.8") }
    var whiteBalance by remember { mutableStateOf("5600K") }
    var exposureCompensation by remember { mutableStateOf("0.0 EV") }
    var colorSpace by remember { mutableStateOf("S-Gamut3.Cine") }
    var gammaProfile by remember { mutableStateOf("S-Log3") }
    var recordingFormat by remember { mutableStateOf("XAVC-I 422 10-bit") }

    // VFX Defaults
    var plateType by remember { mutableStateOf("Reference / Clean Plate") }
    var environment by remember { mutableStateOf("Interior Stage") }
    var lightingNotes by remember { mutableStateOf("Key 5600K, fill 4000K") }
    var generalVfxNotes by remember { mutableStateOf("") }
    var cameraHeight by remember { mutableStateOf("1.5 m") }
    var defaultCameraDistance by remember { mutableStateOf("3.0 m") }
    var defaultTrackingNotes by remember { mutableStateOf("Tracking markers placed on set walls") }

    var nameError by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        if (projectId != null) {
            val proj = repository.getProject(projectId)
            if (proj != null) {
                existingProject = proj
                name = proj.name
                client = proj.client
                productionCompany = proj.productionCompany
                productionShow = proj.productionShow
                projectIdCode = proj.projectIdCode
                description = proj.description
                director = proj.director
                vfxSupervisor = proj.vfxSupervisor
                vfxProducer = proj.vfxProducer
                cameraOperator = proj.cameraOperator
                date = proj.date
                location = proj.location

                val c = proj.cameraDefaults
                camManufacturer = c.cameraManufacturer
                camModel = c.cameraModel
                camUnitId = c.cameraUnitId
                lensManufacturer = c.lensManufacturer
                lensModel = c.lensModel
                lensId = c.lensId
                focalLength = c.focalLength
                sensorFormat = c.sensorFormat
                sensorSize = c.sensorSize
                resolution = c.resolution
                frameRate = c.frameRate
                iso = c.iso
                shutterSpeed = c.shutterSpeed
                aperture = c.aperture
                whiteBalance = c.whiteBalance
                exposureCompensation = c.exposureCompensation
                colorSpace = c.colorSpace
                gammaProfile = c.gammaProfile
                recordingFormat = c.recordingFormat

                val v = proj.vfxDefaults
                plateType = v.plateType
                environment = v.environment
                lightingNotes = v.lightingNotes
                generalVfxNotes = v.generalVfxNotes
                cameraHeight = v.cameraHeight
                defaultCameraDistance = v.defaultCameraDistance
                defaultTrackingNotes = v.defaultTrackingNotes
            }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(VfxBlack)) {
                VfxTopBar(
                    title = if (projectId == null) "NEW VFX PROJECT" else "EDIT PROJECT",
                    subtitle = if (projectId == null) "Set project & camera baseline" else name,
                    onBack = onNavigateBack,
                    actions = {
                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    nameError = true
                                    selectedSection = 0
                                    return@Button
                                }
                                scope.launch {
                                    val projectToSave = (existingProject ?: ProjectEntity(name = name.trim())).copy(
                                        name = name.trim(),
                                        client = client.trim(),
                                        productionCompany = productionCompany.trim(),
                                        productionShow = productionShow.trim(),
                                        projectIdCode = projectIdCode.trim(),
                                        description = description.trim(),
                                        director = director.trim(),
                                        vfxSupervisor = vfxSupervisor.trim(),
                                        vfxProducer = vfxProducer.trim(),
                                        cameraOperator = cameraOperator.trim(),
                                        date = date.trim(),
                                        location = location.trim(),
                                        cameraDefaults = CameraSettings(
                                            cameraManufacturer = camManufacturer.trim(),
                                            cameraModel = camModel.trim(),
                                            cameraUnitId = camUnitId.trim(),
                                            lensManufacturer = lensManufacturer.trim(),
                                            lensModel = lensModel.trim(),
                                            lensId = lensId.trim(),
                                            focalLength = focalLength.trim(),
                                            sensorFormat = sensorFormat.trim(),
                                            sensorSize = sensorSize.trim(),
                                            resolution = resolution.trim(),
                                            frameRate = frameRate.trim(),
                                            iso = iso.trim(),
                                            shutterSpeed = shutterSpeed.trim(),
                                            aperture = aperture.trim(),
                                            whiteBalance = whiteBalance.trim(),
                                            exposureCompensation = exposureCompensation.trim(),
                                            colorSpace = colorSpace.trim(),
                                            gammaProfile = gammaProfile.trim(),
                                            recordingFormat = recordingFormat.trim()
                                        ),
                                        vfxDefaults = VfxSettings(
                                            plateType = plateType.trim(),
                                            environment = environment.trim(),
                                            lightingNotes = lightingNotes.trim(),
                                            generalVfxNotes = generalVfxNotes.trim(),
                                            cameraHeight = cameraHeight.trim(),
                                            defaultCameraDistance = defaultCameraDistance.trim(),
                                            defaultTrackingNotes = defaultTrackingNotes.trim()
                                        )
                                    )
                                    repository.saveProject(projectToSave)

                                    // If creating a brand new project, create Day 1 automatically
                                    if (projectId == null) {
                                        repository.createShootingDay(
                                            projectId = projectToSave.id,
                                            date = projectToSave.date.ifBlank { SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date()) },
                                            location = projectToSave.location,
                                            dayNotes = "Initial shooting day."
                                        )
                                    }
                                    onProjectSaved(projectToSave.id)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .testTag("save_project_button")
                        ) {
                            Text("SAVE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                )

                TabRow(
                    selectedTabIndex = selectedSection,
                    containerColor = VfxBlack,
                    contentColor = VfxAmber,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedSection]),
                            color = VfxAmber,
                            height = 2.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedSection == 0,
                        onClick = { selectedSection = 0 },
                        text = {
                            Text(
                                "PROJECT",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (selectedSection == 0) VfxAmber else VfxTextMuted
                            )
                        }
                    )
                    Tab(
                        selected = selectedSection == 1,
                        onClick = { selectedSection = 1 },
                        text = {
                            Text(
                                "CAMERA DEFAULTS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (selectedSection == 1) VfxAmber else VfxTextMuted
                            )
                        }
                    )
                    Tab(
                        selected = selectedSection == 2,
                        onClick = { selectedSection = 2 },
                        text = {
                            Text(
                                "VFX DEFAULTS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (selectedSection == 2) VfxAmber else VfxTextMuted
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedSection) {
                0 -> {
                    item {
                        VfxTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                if (it.isNotBlank()) nameError = false
                            },
                            label = "Project Name *",
                            placeholder = "e.g. Project Falcon",
                            isError = nameError
                        )
                        if (nameError) {
                            Text(
                                "Project Name is required",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VfxTextField(value = client, onValueChange = { client = it }, label = "Client", placeholder = "e.g. XYZ Studios", modifier = Modifier.weight(1f))
                            VfxTextField(value = productionCompany, onValueChange = { productionCompany = it }, label = "Production Co.", placeholder = "e.g. Falcon Films", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VfxTextField(value = productionShow, onValueChange = { productionShow = it }, label = "Production / Show", placeholder = "e.g. Falcon Season 1", modifier = Modifier.weight(1f))
                            VfxTextField(value = projectIdCode, onValueChange = { projectIdCode = it }, label = "Project ID Code", placeholder = "e.g. FLC-2026", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        VfxTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = "Description / VFX Brief",
                            placeholder = "Describe sequence and visual effects scope...",
                            singleLine = false
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VfxTextField(value = director, onValueChange = { director = it }, label = "Director", placeholder = "Director Name", modifier = Modifier.weight(1f))
                            VfxTextField(value = vfxSupervisor, onValueChange = { vfxSupervisor = it }, label = "VFX Supervisor", placeholder = "Supervisor Name", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VfxTextField(value = vfxProducer, onValueChange = { vfxProducer = it }, label = "VFX Producer", placeholder = "Producer Name", modifier = Modifier.weight(1f))
                            VfxTextField(value = cameraOperator, onValueChange = { cameraOperator = it }, label = "Camera Operator", placeholder = "Operator Name", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VfxTextField(value = date, onValueChange = { date = it }, label = "Project Date", placeholder = "dd MMM yyyy", modifier = Modifier.weight(1f))
                            VfxTextField(value = location, onValueChange = { location = it }, label = "Location", placeholder = "Stage 4, Pune", modifier = Modifier.weight(1f))
                        }
                    }
                }
                1 -> {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VfxTextField(value = camManufacturer, onValueChange = { camManufacturer = it }, label = "Camera Manufacturer", placeholder = "Sony", modifier = Modifier.weight(1f))
                            VfxTextField(value = camModel, onValueChange = { camModel = it }, label = "Camera Model", placeholder = "FX6", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VfxTextField(value = camUnitId, onValueChange = { camUnitId = it }, label = "Camera Unit ID", placeholder = "A-CAM", modifier = Modifier.weight(1f))
                            VfxTextField(value = resolution, onValueChange = { resolution = it }, label = "Resolution", placeholder = "4096x2160 (4K DCI)", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VfxTextField(value = lensManufacturer, onValueChange = { lensManufacturer = it }, label = "Lens Manufacturer", placeholder = "Zeiss", modifier = Modifier.weight(1f))
                            VfxTextField(value = lensModel, onValueChange = { lensModel = it }, label = "Lens Model", placeholder = "Supreme Prime", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VfxTextField(value = focalLength, onValueChange = { focalLength = it }, label = "Focal Length", placeholder = "24mm", modifier = Modifier.weight(1f))
                            VfxTextField(value = lensId, onValueChange = { lensId = it }, label = "Lens ID", placeholder = "SP-24", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VfxTextField(value = iso, onValueChange = { iso = it }, label = "ISO", placeholder = "800", modifier = Modifier.weight(1f))
                            VfxTextField(value = shutterSpeed, onValueChange = { shutterSpeed = it }, label = "Shutter Speed", placeholder = "1/48", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VfxTextField(value = aperture, onValueChange = { aperture = it }, label = "Aperture", placeholder = "f/2.8", modifier = Modifier.weight(1f))
                            VfxTextField(value = frameRate, onValueChange = { frameRate = it }, label = "Frame Rate", placeholder = "24 fps", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VfxTextField(value = whiteBalance, onValueChange = { whiteBalance = it }, label = "White Balance", placeholder = "5600K", modifier = Modifier.weight(1f))
                            VfxTextField(value = exposureCompensation, onValueChange = { exposureCompensation = it }, label = "Exposure Comp", placeholder = "0.0 EV", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VfxTextField(value = colorSpace, onValueChange = { colorSpace = it }, label = "Color Space", placeholder = "S-Gamut3.Cine", modifier = Modifier.weight(1f))
                            VfxTextField(value = gammaProfile, onValueChange = { gammaProfile = it }, label = "Gamma / Log Profile", placeholder = "S-Log3", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VfxTextField(value = sensorFormat, onValueChange = { sensorFormat = it }, label = "Sensor Format", placeholder = "Full Frame 35mm", modifier = Modifier.weight(1f))
                            VfxTextField(value = recordingFormat, onValueChange = { recordingFormat = it }, label = "Recording Format", placeholder = "XAVC-I 422 10-bit", modifier = Modifier.weight(1f))
                        }
                    }
                }
                2 -> {
                    item {
                        VfxTextField(value = plateType, onValueChange = { plateType = it }, label = "Default Plate Type", placeholder = "Reference / Clean Plate")
                    }
                    item {
                        VfxTextField(value = environment, onValueChange = { environment = it }, label = "Environment", placeholder = "Interior Stage with Green Screen")
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VfxTextField(value = cameraHeight, onValueChange = { cameraHeight = it }, label = "Default Camera Height", placeholder = "1.5 m", modifier = Modifier.weight(1f))
                            VfxTextField(value = defaultCameraDistance, onValueChange = { defaultCameraDistance = it }, label = "Default Distance", placeholder = "3.0 m", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        VfxTextField(value = lightingNotes, onValueChange = { lightingNotes = it }, label = "Default Lighting Notes", placeholder = "Key 5600K, fill 4000K", singleLine = false)
                    }
                    item {
                        VfxTextField(value = defaultTrackingNotes, onValueChange = { defaultTrackingNotes = it }, label = "Default Tracking Notes", placeholder = "Marker tape pattern & coordinates", singleLine = false)
                    }
                    item {
                        VfxTextField(value = generalVfxNotes, onValueChange = { generalVfxNotes = it }, label = "General VFX Notes", placeholder = "Special requirements, CG assets reference...", singleLine = false)
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
