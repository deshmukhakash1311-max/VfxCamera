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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CaptureEntity
import com.example.data.repository.VfxRepository
import com.example.ui.components.VfxCard
import com.example.ui.components.VfxTopBar
import com.example.ui.theme.VfxAmber
import com.example.ui.theme.VfxBlack
import com.example.ui.theme.VfxBorder
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
fun ShotListScreen(
    repository: VfxRepository,
    dayId: String,
    onNavigateBack: () -> Unit,
    onCaptureClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val day by repository.observeShootingDay(dayId).collectAsState(initial = null)
    val captures by repository.getCapturesForDay(dayId).collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var sortDescending by remember { mutableStateOf(false) }
    var capturePendingDelete by remember { mutableStateOf<CaptureEntity?>(null) }

    val filteredCaptures = captures.filter {
        it.shotNumber.contains(searchQuery, ignoreCase = true) ||
        it.scene.contains(searchQuery, ignoreCase = true) ||
        it.take.contains(searchQuery, ignoreCase = true) ||
        it.shotNotes.contains(searchQuery, ignoreCase = true)
    }.let { list ->
        if (sortDescending) list.reversed() else list
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(VfxBlack)) {
                VfxTopBar(
                    title = if (day != null) "DAY ${day!!.dayNumber} SHOT LIST" else "SHOT LIST",
                    subtitle = "${captures.size} Total Captures",
                    onBack = onNavigateBack,
                    actions = {
                        IconButton(onClick = { sortDescending = !sortDescending }, modifier = Modifier.testTag("action_sort_shots")) {
                            Icon(
                                Icons.Default.Sort,
                                contentDescription = "Sort",
                                tint = if (sortDescending) VfxAmber else VfxTextSecondary
                            )
                        }
                    }
                )

                // Search Bar
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search shots by number, scene, notes...", color = VfxTextMuted, style = MaterialTheme.typography.bodyMedium) },
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
                        modifier = Modifier.fillMaxWidth().testTag("search_shots_input")
                    )
                }
            }
        },
        containerColor = VfxBlack,
        modifier = modifier
    ) { innerPadding ->
        if (filteredCaptures.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "No shots match '$searchQuery'" else "No shots recorded yet for this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VfxTextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("shot_list_items"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCaptures, key = { it.id }) { capture ->
                    VfxCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shot_row_${capture.shotNumber}"),
                        onClick = { onCaptureClick(capture.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail
                            val file = File(capture.thumbnailPath ?: capture.imagePath)
                            Box(
                                modifier = Modifier
                                    .size(width = 72.dp, height = 54.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(VfxBlack)
                                    .border(1.dp, VfxBorder, RoundedCornerShape(4.dp))
                            ) {
                                if (file.exists()) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = capture.shotNumber,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = capture.shotNumber,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = VfxAmber
                                    )
                                    if (capture.isReference) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.Bookmark, contentDescription = "Reference", tint = VfxAmber, modifier = Modifier.size(12.dp))
                                    }
                                }
                                Text(
                                    text = "${capture.scene.ifBlank { "Scene" }} · ${capture.take}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = VfxTextPrimary
                                )
                                val timeStr = SimpleDateFormat("dd MMM · HH:mm", Locale.US).format(Date(capture.createdAt))
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VfxTextMuted
                                )
                            }

                            IconButton(
                                onClick = { capturePendingDelete = capture },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = VfxTextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    if (capturePendingDelete != null) {
        val shot = capturePendingDelete!!
        AlertDialog(
            onDismissRequest = { capturePendingDelete = null },
            title = { Text("Delete ${shot.shotNumber}?", color = VfxTextPrimary) },
            text = { Text("Are you sure you want to delete ${shot.shotNumber}? This cannot be undone.", color = VfxTextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDel = capturePendingDelete
                        capturePendingDelete = null
                        if (toDel != null) {
                            scope.launch { repository.deleteCapture(toDel) }
                        }
                    }
                ) {
                    Text("Delete", color = VfxRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { capturePendingDelete = null }) {
                    Text("Cancel", color = VfxTextPrimary)
                }
            },
            containerColor = VfxSurface,
            shape = RoundedCornerShape(8.dp)
        )
    }
}
