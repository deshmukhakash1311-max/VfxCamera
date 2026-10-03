package com.example.ui.reports

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.ExportedReportEntity
import com.example.data.model.ProjectEntity
import com.example.data.repository.VfxRepository
import com.example.pdf.PdfExportOptions
import com.example.pdf.VfxPdfReportGenerator
import com.example.ui.components.VfxCard
import com.example.ui.components.VfxTopBar
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
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    repository: VfxRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reports by repository.allReports.collectAsState(initial = emptyList())
    var reportPendingDelete by remember { mutableStateOf<ExportedReportEntity?>(null) }

    Scaffold(
        topBar = {
            VfxTopBar(
                title = "EXPORTED REPORTS",
                subtitle = "${reports.size} Generated PDF Documents",
                onBack = onNavigateBack
            )
        },
        containerColor = VfxBlack,
        modifier = modifier
    ) { innerPadding ->
        if (reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = VfxBorder, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No PDF reports generated yet", style = MaterialTheme.typography.titleMedium, color = VfxTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Open any project and tap 'Export PDF' to create a production report.", style = MaterialTheme.typography.bodySmall, color = VfxTextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("reports_list"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reports, key = { it.id }) { report ->
                    VfxCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_card_${report.fileName}")
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = VfxAmber,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = report.fileName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = VfxTextPrimary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${report.projectName} · ${report.layoutType} Layout",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VfxTextSecondary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { reportPendingDelete = report },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = VfxTextMuted, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dateStr = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.US).format(Date(report.createdAt))
                                val sizeKb = report.fileSize / 1024
                                Text(
                                    text = "$dateStr · ${sizeKb} KB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VfxTextMuted
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { openPdfFile(context, File(report.filePath)) },
                                        colors = ButtonDefaults.buttonColors(containerColor = VfxSurface, contentColor = VfxTextPrimary),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("OPEN", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }

                                    Button(
                                        onClick = { sharePdfFile(context, File(report.filePath)) },
                                        colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("SHARE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (reportPendingDelete != null) {
        val r = reportPendingDelete!!
        AlertDialog(
            onDismissRequest = { reportPendingDelete = null },
            title = { Text("Delete Report?", color = VfxTextPrimary) },
            text = { Text("Are you sure you want to delete ${r.fileName}?", color = VfxTextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDel = reportPendingDelete
                        reportPendingDelete = null
                        if (toDel != null) {
                            scope.launch { repository.deleteReport(toDel) }
                        }
                    }
                ) {
                    Text("Delete", color = VfxRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { reportPendingDelete = null }) {
                    Text("Cancel", color = VfxTextPrimary)
                }
            },
            containerColor = VfxSurface,
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun ExportPdfDialog(
    project: ProjectEntity,
    repository: VfxRepository,
    onDismiss: () -> Unit,
    onReportGenerated: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val days by repository.getShootingDays(project.id).collectAsState(initial = emptyList())

    var layoutType by remember { mutableStateOf("Detailed") } // "Detailed", "Compact", "Image Only"
    var incProjectDetails by remember { mutableStateOf(true) }
    var incDayDetails by remember { mutableStateOf(true) }
    var incImages by remember { mutableStateOf(true) }
    var incCameraMetadata by remember { mutableStateOf(true) }
    var incVfxMetadata by remember { mutableStateOf(true) }
    var incNotes by remember { mutableStateOf(true) }
    var incLocation by remember { mutableStateOf(true) }

    var selectedDayId by remember { mutableStateOf<String?>(null) } // null = All Days

    var isGenerating by remember { mutableStateOf(false) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        title = {
            Text(
                text = if (generatedFile == null) "EXPORT PDF REPORT" else "REPORT EXPORTED",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = if (generatedFile == null) VfxAmber else VfxGreen
            )
        },
        text = {
            if (isGenerating) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = VfxAmber)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Compiling PDF pages & metadata...", style = MaterialTheme.typography.bodyMedium, color = VfxTextPrimary)
                    }
                }
            } else if (generatedFile != null) {
                // Success View (Requirement 27)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VfxGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(project.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = VfxTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = generatedFile!!.name,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = VfxAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "File size: ${generatedFile!!.length() / 1024} KB",
                        style = MaterialTheme.typography.labelSmall,
                        color = VfxTextMuted
                    )
                }
            } else {
                // Configuration View (Requirement 25)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("REPORT LAYOUT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = VfxAmber)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Detailed", "Compact", "Image Only").forEach { layout ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (layoutType == layout) VfxAmber else VfxSurface)
                                        .border(1.dp, if (layoutType == layout) VfxAmber else VfxBorder, RoundedCornerShape(4.dp))
                                        .clickable { layoutType = layout }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = layout,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (layoutType == layout) VfxBlack else VfxTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("INCLUDE SECTIONS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = VfxAmber)
                    }

                    item {
                        OptionCheck("Project & Client Details", incProjectDetails) { incProjectDetails = it }
                    }
                    item {
                        OptionCheck("Shooting Day Details", incDayDetails) { incDayDetails = it }
                    }
                    item {
                        OptionCheck("All Capture Images", incImages) { incImages = it }
                    }
                    item {
                        OptionCheck("Camera Metadata", incCameraMetadata) { incCameraMetadata = it }
                    }
                    item {
                        OptionCheck("VFX & Tracking Information", incVfxMetadata) { incVfxMetadata = it }
                    }
                    item {
                        OptionCheck("Shot & VFX Notes", incNotes) { incNotes = it }
                    }
                    item {
                        OptionCheck("Location & Geodata", incLocation) { incLocation = it }
                    }
                }
            }
        },
        confirmButton = {
            if (isGenerating) {
                // empty
            } else if (generatedFile != null) {
                Row {
                    Button(
                        onClick = { openPdfFile(context, generatedFile!!) },
                        colors = ButtonDefaults.buttonColors(containerColor = VfxSurface, contentColor = VfxTextPrimary),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.testTag("pdf_success_open")
                    ) {
                        Text("OPEN")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { sharePdfFile(context, generatedFile!!) },
                        colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.testTag("pdf_success_share")
                    ) {
                        Text("SHARE", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = {
                        isGenerating = true
                        scope.launch {
                            try {
                                val shootingDaysList = repository.getShootingDaysList(project.id)
                                val capturesByDay = mutableMapOf<String, List<com.example.data.model.CaptureEntity>>()
                                for (d in shootingDaysList) {
                                    capturesByDay[d.id] = repository.getCapturesListForDay(d.id)
                                }

                                val options = PdfExportOptions(
                                    layoutType = layoutType,
                                    includeProjectDetails = incProjectDetails,
                                    includeDayDetails = incDayDetails,
                                    includeImages = incImages,
                                    includeCameraMetadata = incCameraMetadata,
                                    includeVfxMetadata = incVfxMetadata,
                                    includeNotes = incNotes,
                                    includeLocation = incLocation,
                                    selectedDayId = selectedDayId
                                )

                                val file = VfxPdfReportGenerator.generateReport(
                                    context = context,
                                    project = project,
                                    days = shootingDaysList,
                                    capturesByDay = capturesByDay,
                                    options = options
                                )

                                // Save report record to DB
                                val totalShots = capturesByDay.values.sumOf { it.size }
                                val reportEntity = ExportedReportEntity(
                                    projectId = project.id,
                                    projectName = project.name,
                                    fileName = file.name,
                                    filePath = file.absolutePath,
                                    fileSize = file.length(),
                                    pageCount = 1,
                                    shotCount = totalShots,
                                    layoutType = layoutType
                                )
                                repository.saveReport(reportEntity)

                                generatedFile = file
                                isGenerating = false
                                onReportGenerated(file)
                            } catch (e: Exception) {
                                isGenerating = false
                                errorMessage = e.message ?: "Failed to generate report"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VfxAmber, contentColor = VfxBlack),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.testTag("confirm_export_pdf_button")
                ) {
                    Text("GENERATE PDF", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isGenerating) {
                TextButton(onClick = onDismiss) {
                    Text(if (generatedFile != null) "DONE" else "CANCEL", color = VfxTextPrimary)
                }
            }
        },
        containerColor = VfxSurface,
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun OptionCheck(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = VfxAmber,
                uncheckedColor = VfxBorder,
                checkmarkColor = VfxBlack
            )
        )
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = VfxTextPrimary)
    }
}

fun openPdfFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open VFX Report"))
    } catch (_: Exception) {
        // App chooser fallback
    }
}

fun sharePdfFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share VFX Report"))
    } catch (_: Exception) {
        // Share chooser fallback
    }
}
