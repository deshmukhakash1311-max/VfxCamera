package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InheritanceSource
import com.example.ui.theme.VfxAmber
import com.example.ui.theme.VfxAmberBg
import com.example.ui.theme.VfxAmberDim
import com.example.ui.theme.VfxBlack
import com.example.ui.theme.VfxBorder
import com.example.ui.theme.VfxBorderSubtle
import com.example.ui.theme.VfxCyan
import com.example.ui.theme.VfxPanel
import com.example.ui.theme.VfxRed
import com.example.ui.theme.VfxSurface
import com.example.ui.theme.VfxSurfaceVariant
import com.example.ui.theme.VfxTextMuted
import com.example.ui.theme.VfxTextPrimary
import com.example.ui.theme.VfxTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VfxTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = VfxTextPrimary,
                    maxLines = 1
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = VfxTextMuted,
                        maxLines = 1
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("top_bar_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VfxTextPrimary
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = VfxBlack,
            titleContentColor = VfxTextPrimary,
            actionIconContentColor = VfxTextPrimary
        ),
        modifier = modifier.border(0.dp, Color.Transparent)
    )
}

@Composable
fun VfxCard(
    modifier: Modifier = Modifier,
    borderColor: Color = VfxBorder,
    backgroundColor: Color = VfxCardBg,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(6.dp)
    Surface(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(modifier = Modifier.padding(14.dp)) {
            content()
        }
    }
}

private val VfxCardBg = Color(0xFF13171F)

@Composable
fun VfxTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingText: String? = null,
    isError: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            ),
            color = VfxTextMuted,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                if (placeholder.isNotBlank()) {
                    Text(
                        text = placeholder,
                        color = VfxTextMuted.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    )
                }
            },
            singleLine = singleLine,
            leadingIcon = if (leadingIcon != null) {
                { Icon(leadingIcon, contentDescription = null, tint = VfxAmberDim, modifier = Modifier.size(18.dp)) }
            } else null,
            trailingIcon = if (trailingText != null) {
                { Text(trailingText, color = VfxTextMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 12.dp)) }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VfxSurfaceVariant,
                unfocusedContainerColor = VfxSurface,
                focusedBorderColor = VfxAmber,
                unfocusedBorderColor = VfxBorder,
                focusedTextColor = VfxTextPrimary,
                unfocusedTextColor = VfxTextPrimary,
                cursorColor = VfxAmber
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_${label.lowercase().replace(" ", "_")}"),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            isError = isError
        )
    }
}

@Composable
fun MetadataBadge(
    source: InheritanceSource,
    modifier: Modifier = Modifier
) {
    val (label, bg, fg, border) = when (source) {
        InheritanceSource.PROJECT_DEFAULT -> Quad("INHERITED (PROJECT)", Color(0xFF19232F), Color(0xFF7DD3FC), Color(0xFF224363))
        InheritanceSource.DAY_OVERRIDE -> Quad("DAY OVERRIDE", Color(0xFF2B2212), Color(0xFFFBBF24), Color(0xFF65481B))
        InheritanceSource.SHOT_OVERRIDE -> Quad("SHOT OVERRIDE", Color(0xFF2F1D1B), Color(0xFFF87171), Color(0xFF672A24))
        InheritanceSource.ORIGINAL_CAMERA -> Quad("ACTUAL SENSOR", Color(0xFF142820), Color(0xFF34D399), Color(0xFF1E523A))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun MetadataDisplayRow(
    label: String,
    value: String,
    source: InheritanceSource,
    originalValue: String? = null,
    onReset: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = VfxTextMuted
            )
            MetadataBadge(source = source)
        }

        Spacer(modifier = Modifier.height(3.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.ifBlank { "Not set" },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                ),
                color = if (source == InheritanceSource.SHOT_OVERRIDE || source == InheritanceSource.DAY_OVERRIDE) VfxAmber else VfxTextPrimary
            )

            if (onReset != null && (source == InheritanceSource.DAY_OVERRIDE || source == InheritanceSource.SHOT_OVERRIDE)) {
                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("reset_$label")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset to Default",
                        tint = VfxAmber,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (originalValue != null && originalValue.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Sensor Original: ",
                    style = MaterialTheme.typography.labelSmall,
                    color = VfxTextMuted
                )
                Text(
                    text = originalValue,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color(0xFF34D399)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(VfxBorderSubtle))
    }
}
