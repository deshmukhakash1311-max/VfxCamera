package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.CaptureEntity
import com.example.ui.theme.VfxAmber
import com.example.ui.theme.VfxBlack
import com.example.ui.theme.VfxBorder
import com.example.ui.theme.VfxPanel
import com.example.ui.theme.VfxRed
import com.example.ui.theme.VfxSurface
import com.example.ui.theme.VfxTextMuted
import com.example.ui.theme.VfxTextPrimary
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThumbnailStrip(
    captures: List<CaptureEntity>,
    selectedCaptureId: String?,
    onCaptureClick: (CaptureEntity) -> Unit,
    onEditClick: (CaptureEntity) -> Unit,
    onDeleteClick: (CaptureEntity) -> Unit,
    onToggleReference: (CaptureEntity) -> Unit,
    onNewShotClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var capturePendingDelete by remember { mutableStateOf<CaptureEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(VfxPanel.copy(alpha = 0.92f))
            .border(1.dp, VfxBorder.copy(alpha = 0.6f))
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CAPTURES",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = VfxAmber
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(VfxSurface)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${captures.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = VfxTextMuted
                    )
                }
            }

            if (captures.isNotEmpty()) {
                Text(
                    text = "Tap to open · Long-press for options",
                    style = MaterialTheme.typography.labelSmall,
                    color = VfxTextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("thumbnail_strip"),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(captures, key = { it.id }) { capture ->
                val isSelected = capture.id == selectedCaptureId
                var showMenu by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .size(width = 76.dp, height = 76.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(VfxBlack)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) VfxAmber else VfxBorder,
                            shape = RoundedCornerShape(5.dp)
                        )
                        .combinedClickable(
                            onClick = { onCaptureClick(capture) },
                            onLongClick = { showMenu = true }
                        )
                        .testTag("thumb_${capture.shotNumber}")
                ) {
                    val file = File(capture.thumbnailPath ?: capture.imagePath)
                    if (file.exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(file)
                                .crossfade(true)
                                .build(),
                            contentDescription = capture.shotNumber,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("IMG", style = MaterialTheme.typography.labelSmall, color = VfxTextMuted)
                        }
                    }

                    // Reference ribbon
                    if (capture.isReference) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(VfxAmber)
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = "Reference",
                                tint = VfxBlack,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }

                    // Bottom label bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.78f))
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = capture.shotNumber.replace("SHOT ", "S"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = if (isSelected) VfxAmber else VfxTextPrimary
                        )
                    }

                    // Dropdown menu
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(VfxSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("View & Edit Details", color = VfxTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = VfxAmber) },
                            onClick = {
                                showMenu = false
                                onEditClick(capture)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (capture.isReference) "Remove Reference" else "Set as Reference",
                                    color = VfxTextPrimary
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null, tint = VfxAmber) },
                            onClick = {
                                showMenu = false
                                onToggleReference(capture)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Shot", color = VfxRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = VfxRed) },
                            onClick = {
                                showMenu = false
                                capturePendingDelete = capture
                            }
                        )
                    }
                }
            }

            if (onNewShotClick != null) {
                item {
                    Box(
                        modifier = Modifier
                            .size(width = 76.dp, height = 76.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(VfxSurface)
                            .border(1.dp, VfxBorder, RoundedCornerShape(5.dp))
                            .combinedClickable(onClick = onNewShotClick)
                            .testTag("thumb_add_shot"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, contentDescription = "New Shot", tint = VfxAmber, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("TRIGGER", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = VfxTextMuted)
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (capturePendingDelete != null) {
        val shot = capturePendingDelete!!
        AlertDialog(
            onDismissRequest = { capturePendingDelete = null },
            title = {
                Text(
                    text = "Delete ${shot.shotNumber}?",
                    color = VfxTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete ${shot.shotNumber} (${shot.scene.ifBlank { "Scene" }})? This action cannot be undone.",
                    color = VfxTextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDel = capturePendingDelete
                        capturePendingDelete = null
                        if (toDel != null) onDeleteClick(toDel)
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
