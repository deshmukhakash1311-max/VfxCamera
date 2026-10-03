package com.example.ui.settings

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.CameraMetadataReader
import com.example.data.repository.VfxRepository
import com.example.ui.components.VfxCard
import com.example.ui.components.VfxTopBar
import com.example.ui.theme.VfxAmber
import com.example.ui.theme.VfxBlack
import com.example.ui.theme.VfxBorder
import com.example.ui.theme.VfxCyan
import com.example.ui.theme.VfxGreen
import com.example.ui.theme.VfxPanel
import com.example.ui.theme.VfxSurface
import com.example.ui.theme.VfxTextMuted
import com.example.ui.theme.VfxTextPrimary
import com.example.ui.theme.VfxTextSecondary
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsScreen(
    repository: VfxRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cameraSpecs by remember { mutableStateOf<List<String>>(emptyList()) }
    var storageInfo by remember { mutableStateOf("Calculating...") }

    LaunchedEffect(Unit) {
        cameraSpecs = CameraMetadataReader.getDeviceCameraSpecs(context)
        val capturesDir = File(context.filesDir, "captures")
        val reportsDir = File(context.filesDir, "reports")
        val capSize = capturesDir.walk().filter { it.isFile }.map { it.length() }.sum()
        val repSize = reportsDir.walk().filter { it.isFile }.map { it.length() }.sum()
        val totalMb = (capSize + repSize) / (1024 * 1024)
        storageInfo = "${totalMb} MB used (Captures: ${capSize / 1024} KB, Reports: ${repSize / 1024} KB)"
    }

    Scaffold(
        topBar = {
            VfxTopBar(
                title = "SETTINGS & SYSTEM",
                subtitle = "Hardware Diagnostics & Offline Storage",
                onBack = onNavigateBack
            )
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
            item {
                VfxCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = VfxAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "VFX CAPTURE ARCHITECTURE",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = VfxAmber
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Hierarchical Metadata Inheritance:\nProject Defaults  →  Day Overrides  →  Shot Overrides\n\nAll data is stored offline on device in local SQLite / Room database. Image frames and PDF reports are stored in app-private sandboxed storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VfxTextSecondary
                        )
                    }
                }
            }

            item {
                VfxCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = VfxCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "LOCAL STORAGE USAGE",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = VfxCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = storageInfo,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = VfxTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Original images and PDF reports remain on device even across app restarts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VfxTextMuted
                        )
                    }
                }
            }

            item {
                VfxCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = VfxGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "CAMERA HARDWARE TELEMETRY",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = VfxGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (cameraSpecs.isEmpty()) {
                            Text(
                                text = "Device Camera2 characteristics will be displayed when available on physical hardware.",
                                style = MaterialTheme.typography.bodySmall,
                                color = VfxTextMuted
                            )
                        } else {
                            cameraSpecs.forEach { spec ->
                                Text(
                                    text = spec,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = VfxTextPrimary,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        scope.launch {
                            val result = repository.loadDemoProject()
                            Toast.makeText(
                                context,
                                if (result.alreadyLoaded) "Demo project is already loaded. Delete it to load it again."
                                else "Demo project loaded",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VfxSurface, contentColor = VfxAmber),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().testTag("button_load_demo_project")
                ) {
                    Text("LOAD DEMO PROJECT", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
