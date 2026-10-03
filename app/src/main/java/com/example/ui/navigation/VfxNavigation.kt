package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.VfxRepository
import com.example.ui.camera.VfxCameraScreen
import com.example.ui.capture.CaptureDetailsScreen
import com.example.ui.capture.ShotListScreen
import com.example.ui.projects.CreateEditProjectScreen
import com.example.ui.projects.ProjectOverviewScreen
import com.example.ui.projects.ProjectsListScreen
import com.example.ui.reports.ExportPdfDialog
import com.example.ui.reports.ReportsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.shootingday.DayDetailsScreen
import com.example.ui.theme.VfxAmber
import com.example.ui.theme.VfxBlack
import com.example.ui.theme.VfxBorder
import com.example.ui.theme.VfxPanel
import com.example.ui.theme.VfxSurface
import com.example.ui.theme.VfxTextMuted
import com.example.ui.theme.VfxTextPrimary
import com.example.ui.theme.VfxTextSecondary

sealed class Screen {
    object ProjectsList : Screen()
    object ReportsList : Screen()
    object Settings : Screen()
    data class Camera(val projectId: String, val dayId: String?) : Screen()
    data class CreateEditProject(val projectId: String? = null) : Screen()
    data class ProjectOverview(val projectId: String) : Screen()
    data class DayDetails(val projectId: String, val dayId: String) : Screen()
    data class ShotList(val dayId: String) : Screen()
    data class CaptureDetails(val captureId: String) : Screen()
}

enum class BottomTab(val title: String, val icon: ImageVector) {
    PROJECTS("PROJECTS", Icons.Default.Folder),
    CAMERA("CAMERA", Icons.Default.CameraAlt),
    REPORTS("REPORTS", Icons.Default.PictureAsPdf),
    SETTINGS("SETTINGS", Icons.Default.Settings)
}

@Composable
fun VfxAppNavigation(
    repository: VfxRepository,
    modifier: Modifier = Modifier
) {
    val activeProjects by repository.activeProjects.collectAsState(initial = emptyList())

    // Backstack management
    var backStack by remember { mutableStateOf(listOf<Screen>(Screen.ProjectsList)) }
    val currentScreen = backStack.lastOrNull() ?: Screen.ProjectsList

    var currentTab by remember { mutableStateOf(BottomTab.PROJECTS) }
    var exportPdfProjectId by remember { mutableStateOf<String?>(null) }
    var exportPdfDayId by remember { mutableStateOf<String?>(null) }

    fun navigateTo(screen: Screen) {
        backStack = backStack + screen
    }

    fun popBack() {
        if (backStack.size > 1) {
            backStack = backStack.dropLast(1)
        }
    }

    val isFullscreenCamera = currentScreen is Screen.Camera
    val showBottomBar = !isFullscreenCamera && (
        currentScreen is Screen.ProjectsList ||
        currentScreen is Screen.ReportsList ||
        currentScreen is Screen.Settings ||
        currentScreen is Screen.ProjectOverview
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = VfxBlack,
                    contentColor = VfxTextPrimary,
                    modifier = Modifier
                        .border(1.dp, VfxBorder)
                        .testTag("main_bottom_nav")
                ) {
                    BottomTab.values().forEach { tab ->
                        val selected = when (tab) {
                            BottomTab.PROJECTS -> currentScreen is Screen.ProjectsList || currentScreen is Screen.ProjectOverview
                            BottomTab.CAMERA -> currentScreen is Screen.Camera
                            BottomTab.REPORTS -> currentScreen is Screen.ReportsList
                            BottomTab.SETTINGS -> currentScreen is Screen.Settings
                        }

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                currentTab = tab
                                when (tab) {
                                    BottomTab.PROJECTS -> {
                                        backStack = listOf(Screen.ProjectsList)
                                    }
                                    BottomTab.CAMERA -> {
                                        val proj = activeProjects.firstOrNull()
                                        if (proj != null) {
                                            backStack = listOf(Screen.ProjectsList, Screen.Camera(proj.id, null))
                                        } else {
                                            backStack = listOf(Screen.ProjectsList)
                                        }
                                    }
                                    BottomTab.REPORTS -> {
                                        backStack = listOf(Screen.ProjectsList, Screen.ReportsList)
                                    }
                                    BottomTab.SETTINGS -> {
                                        backStack = listOf(Screen.ProjectsList, Screen.Settings)
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    tab.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 10.sp
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VfxAmber,
                                unselectedIconColor = VfxTextMuted,
                                selectedTextColor = VfxAmber,
                                unselectedTextColor = VfxTextMuted,
                                indicatorColor = Color(0x20F59E0B)
                            )
                        )
                    }
                }
            }
        },
        containerColor = VfxBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreenCamera) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.ProjectsList -> {
                    ProjectsListScreen(
                        repository = repository,
                        onProjectClick = { projId -> navigateTo(Screen.ProjectOverview(projId)) },
                        onCreateProjectClick = { navigateTo(Screen.CreateEditProject(null)) },
                        onExportPdfClick = { projId -> exportPdfProjectId = projId }
                    )
                }

                is Screen.ProjectOverview -> {
                    BackHandler { popBack() }
                    ProjectOverviewScreen(
                        repository = repository,
                        projectId = screen.projectId,
                        onNavigateBack = { popBack() },
                        onEditProject = { projId -> navigateTo(Screen.CreateEditProject(projId)) },
                        onDayClick = { dayId -> navigateTo(Screen.DayDetails(screen.projectId, dayId)) },
                        onOpenCameraForDay = { projId, dayId -> navigateTo(Screen.Camera(projId, dayId)) },
                        onExportPdf = { projId -> exportPdfProjectId = projId }
                    )
                }

                is Screen.CreateEditProject -> {
                    BackHandler { popBack() }
                    CreateEditProjectScreen(
                        repository = repository,
                        projectId = screen.projectId,
                        onNavigateBack = { popBack() },
                        onProjectSaved = { projId ->
                            backStack = listOf(Screen.ProjectsList, Screen.ProjectOverview(projId))
                        }
                    )
                }

                is Screen.DayDetails -> {
                    BackHandler { popBack() }
                    DayDetailsScreen(
                        repository = repository,
                        projectId = screen.projectId,
                        dayId = screen.dayId,
                        onNavigateBack = { popBack() },
                        onOpenCamera = { navigateTo(Screen.Camera(screen.projectId, screen.dayId)) },
                        onExportDailyReport = {
                            exportPdfProjectId = screen.projectId
                            exportPdfDayId = screen.dayId
                        },
                        onCaptureClick = { capId -> navigateTo(Screen.CaptureDetails(capId)) },
                        onOpenShotList = { navigateTo(Screen.ShotList(screen.dayId)) }
                    )
                }

                is Screen.Camera -> {
                    BackHandler { popBack() }
                    VfxCameraScreen(
                        repository = repository,
                        projectId = screen.projectId,
                        initialDayId = screen.dayId,
                        onNavigateBack = { popBack() },
                        onOpenCaptureDetails = { capId -> navigateTo(Screen.CaptureDetails(capId)) },
                        onOpenDayDetails = { dayId -> navigateTo(Screen.DayDetails(screen.projectId, dayId)) }
                    )
                }

                is Screen.CaptureDetails -> {
                    BackHandler { popBack() }
                    CaptureDetailsScreen(
                        repository = repository,
                        captureId = screen.captureId,
                        onNavigateBack = { popBack() }
                    )
                }

                is Screen.ShotList -> {
                    BackHandler { popBack() }
                    ShotListScreen(
                        repository = repository,
                        dayId = screen.dayId,
                        onNavigateBack = { popBack() },
                        onCaptureClick = { capId -> navigateTo(Screen.CaptureDetails(capId)) }
                    )
                }

                is Screen.ReportsList -> {
                    BackHandler { popBack() }
                    ReportsScreen(
                        repository = repository,
                        onNavigateBack = { popBack() }
                    )
                }

                is Screen.Settings -> {
                    BackHandler { popBack() }
                    SettingsScreen(
                        repository = repository,
                        onNavigateBack = { popBack() }
                    )
                }
            }

            // Export PDF modal overlay
            if (exportPdfProjectId != null) {
                val proj = activeProjects.find { it.id == exportPdfProjectId }
                if (proj != null) {
                    ExportPdfDialog(
                        project = proj,
                        repository = repository,
                        initialDayId = exportPdfDayId,
                        lockToDailyReport = (exportPdfDayId != null),
                        onDismiss = {
                            exportPdfProjectId = null
                            exportPdfDayId = null
                        },
                        onReportGenerated = {
                            // Dialog displays success view with Open/Share
                        }
                    )
                }
            }
        }
    }
}
