package com.example.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.repository.VfxRepository
import com.example.ui.components.VfxCard
import com.example.ui.components.VfxTopBar
import com.example.ui.theme.VfxAmber
import com.example.ui.theme.VfxAmberDim
import com.example.ui.theme.VfxBlack
import com.example.ui.theme.VfxBorder
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
fun ProjectsListScreen(
    repository: VfxRepository,
    onProjectClick: (String) -> Unit,
    onCreateProjectClick: () -> Unit,
    onExportPdfClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: PROJECTS, 1: ARCHIVE
    var searchQuery by remember { mutableStateOf("") }
    var projectPendingDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    val activeProjects by repository.activeProjects.collectAsState(initial = emptyList())
    val archivedProjects by repository.archivedProjects.collectAsState(initial = emptyList())

    val displayedProjects = (if (selectedTab == 0) activeProjects else archivedProjects).filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.client.contains(searchQuery, ignoreCase = true) ||
        it.productionCompany.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(VfxBlack)) {
                VfxTopBar(
                    title = "VFX CAPTURE",
                    subtitle = "Production Metadata & Scene Logging"
                )

                // Tabs: PROJECTS / ARCHIVE
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
                    },
                    divider = {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(VfxBorder))
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "PROJECTS (${activeProjects.size})",
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
                                "ARCHIVE (${archivedProjects.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (selectedTab == 1) VfxAmber else VfxTextMuted
                            )
                        }
                    )
                }

                // Search Bar
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search projects by name, client...", color = VfxTextMuted, style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VfxTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = VfxSurface,
                            unfocusedContainerColor = VfxSurface,
                            focusedBorderColor = VfxAmber,
                            unfocusedBorderColor = VfxBorder,
                            focusedTextColor = VfxTextPrimary,
                            unfocusedTextColor = VfxTextPrimary,
                            cursorColor = VfxAmber
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().testTag("search_projects_input")
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateProjectClick,
                containerColor = VfxAmber,
                contentColor = VfxBlack,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("fab_create_project")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Project")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "NEW PROJECT",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        containerColor = VfxBlack,
        modifier = modifier
    ) { innerPadding ->
        if (displayedProjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Movie,
                        contentDescription = null,
                        tint = VfxBorder,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = "No matching projects",
                            style = MaterialTheme.typography.titleMedium,
                            color = VfxTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try a different search term.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VfxTextMuted
                        )
                    } else if (selectedTab == 0) {
                        Text(
                            text = "NO PROJECTS",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = VfxTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Create your first VFX project.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VfxTextSecondary
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        androidx.compose.material3.Button(
                            onClick = onCreateProjectClick,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = VfxAmber,
                                contentColor = VfxBlack
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.testTag("empty_create_project_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ CREATE PROJECT", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                scope.launch {
                                    repository.loadDemoProject()
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = VfxAmber
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VfxBorder),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.testTag("empty_load_demo_button")
                        ) {
                            Text("LOAD DEMO PROJECT", fontWeight = FontWeight.Medium)
                        }
                    } else {
                        Text(
                            text = "No archived projects",
                            style = MaterialTheme.typography.titleMedium,
                            color = VfxTextSecondary
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("projects_list"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayedProjects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        repository = repository,
                        onClick = { onProjectClick(project.id) },
                        onExportPdf = { onExportPdfClick(project.id) },
                        onDelete = { projectPendingDelete = project }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    if (projectPendingDelete != null) {
        val proj = projectPendingDelete!!
        AlertDialog(
            onDismissRequest = { projectPendingDelete = null },
            title = { Text("Delete Project?", color = VfxTextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete '${proj.name}' and all its shooting days and captures? This cannot be undone.",
                    color = VfxTextMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDel = projectPendingDelete
                        projectPendingDelete = null
                        if (toDel != null) {
                            scope.launch {
                                repository.deleteProject(toDel)
                            }
                        }
                    }
                ) {
                    Text("Delete", color = VfxRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectPendingDelete = null }) {
                    Text("Cancel", color = VfxTextPrimary)
                }
            },
            containerColor = VfxSurface,
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun ProjectCard(
    project: ProjectEntity,
    repository: VfxRepository,
    onClick: () -> Unit,
    onExportPdf: () -> Unit,
    onDelete: () -> Unit
) {
    val days by repository.getShootingDays(project.id).collectAsState(initial = emptyList())
    val captures by repository.getCapturesForProject(project.id).collectAsState(initial = emptyList())

    VfxCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_card_${project.name}"),
        onClick = onClick
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = VfxTextPrimary
                        )
                        if (project.name.contains("DEMO", ignoreCase = true)) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(VfxAmber)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    "DEMO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = VfxBlack
                                )
                            }
                        }
                    }
                    if (project.client.isNotBlank() || project.productionCompany.isNotBlank()) {
                        Text(
                            text = listOf(project.client, project.productionCompany).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = VfxAmber
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExportPdf, modifier = Modifier.size(32.dp).testTag("card_export_pdf_${project.name}")) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = VfxTextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = VfxTextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Camera Specs badge strip
            val cam = project.cameraDefaults
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp))
                    .background(VfxPanel)
                    .border(1.dp, VfxBorder)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${cam.cameraManufacturer} ${cam.cameraModel}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = VfxTextPrimary
                )
                Text(
                    text = "${cam.focalLength} · ${cam.aperture} · ISO ${cam.iso}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = VfxAmberDim
                )
                Text(
                    text = cam.colorSpace,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = VfxCyan
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Days & Captures Counters & Last Updated
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${days.size} Shooting ${if (days.size == 1) "Day" else "Days"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = VfxTextSecondary
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = VfxTextMuted
                    )
                    Text(
                        text = "${captures.size} Captures",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = VfxTextSecondary
                    )
                }

                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(project.updatedAt))
                Text(
                    text = "Updated $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = VfxTextMuted
                )
            }
        }
    }
}
