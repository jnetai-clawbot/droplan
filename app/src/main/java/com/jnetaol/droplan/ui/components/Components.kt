package com.jnetaol.droplan.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnetaol.droplan.ui.theme.*
import com.jnetaol.droplan.engine.FileTransferProgress
import com.jnetaol.droplan.data.model.TransferHistory

@Composable
fun GlowButton(
    text: String, icon: ImageVector? = null, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, glowColor: Color = DLPrimary
) {
    val transition = rememberInfiniteTransition(label = "glow")
    val alpha by transition.animateFloat(0.4f, 0.8f, infiniteRepeatable(tween(1500, easing = EaseInOutCubic), RepeatMode.Reverse), label = "a")
    Button(onClick = onClick, enabled = enabled, modifier = modifier.shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = glowColor.copy(alpha = alpha), spotColor = glowColor.copy(alpha = alpha)),
        shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = glowColor.copy(alpha = 0.15f), disabledContainerColor = glowColor.copy(alpha = 0.05f), disabledContentColor = DLTextMuted),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp), tint = glowColor); Spacer(Modifier.width(8.dp)) }
        Text(text, color = glowColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun NeonCard(modifier: Modifier = Modifier, borderColor: Color = DLPrimary.copy(alpha = 0.3f), content: @Composable ColumnScope.() -> Unit) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = DLCard),
        border = BorderStroke(1.dp, borderColor), content = content)
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = DLTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (action != null && onAction != null) TextButton(onAction) { Text(action, color = DLSecondary, fontSize = 14.sp) }
    }
}

@Composable
fun StatusBadge(text: String, color: Color = DLPrimary, modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0.1f))), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, Modifier.size(64.dp), tint = DLTextMuted.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))
        Text(title, color = DLTextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = DLTextMuted, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCard(
    deviceName: String,
    deviceId: String,
    osType: String,
    isOnline: Boolean,
    isTrusted: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = DLCard),
        border = BorderStroke(1.dp, if (isOnline) DLNeonCyan.copy(alpha = 0.4f) else DLSurfaceVariant),
        onClick = onTap) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).shadow(8.dp, CircleShape, ambientColor = if (isOnline) DLNeonCyan else DLTextMuted)
                .clip(CircleShape).background(if (isOnline) DLPrimary.copy(alpha = 0.2f) else DLSurfaceVariant), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Devices, null, tint = if (isOnline) DLNeonCyan else DLTextMuted, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(deviceName, color = DLTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(osType, color = DLTextMuted, fontSize = 12.sp)
                    if (isTrusted) { Spacer(Modifier.width(8.dp)); Icon(Icons.Default.VerifiedUser, null, Modifier.size(14.dp), tint = DLNeonTeal) }
                }
            }
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (isOnline) DLSuccess else DLTextMuted.copy(alpha = 0.3f)))
        }
    }
}

@Composable
fun TransferProgressCard(transferProgress: FileTransferProgress, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = DLCard),
        border = BorderStroke(1.dp, DLSurfaceVariant)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (transferProgress.direction == "send") Icons.Default.Upload else Icons.Default.Download, null, Modifier.size(20.dp), tint = if (transferProgress.direction == "send") DLNeonCyan else DLNeonTeal)
                    Spacer(Modifier.width(8.dp))
                    Text(transferProgress.fileName, color = DLTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                StatusBadge(transferProgress.status.uppercase(), if (transferProgress.status == "completed") DLSuccess else if (transferProgress.status == "failed") DLError else DLNeonCyan)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { transferProgress.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = if (transferProgress.status == "completed") DLSuccess else if (transferProgress.status == "failed") DLError else DLPrimary,
                trackColor = DLSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${transferProgress.progress}%", color = DLTextSecondary, fontSize = 12.sp)
                if (transferProgress.speedKbps > 0) Text("${transferProgress.speedKbps} KB/s", color = DLTextMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun HistoryItem(entry: TransferHistory, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = DLCard),
        border = BorderStroke(1.dp, DLSurfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val icon = if (entry.direction == "send") Icons.Default.Upload else Icons.Default.Download
            val iconColor = if (entry.direction == "send") DLNeonCyan else DLNeonTeal
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(iconColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(18.dp), tint = iconColor)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.fileName, color = DLTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${entry.peerName}  (${entry.direction.uppercase()})", color = DLTextMuted, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatFileSize(entry.fileSize), color = DLTextSecondary, fontSize = 12.sp)
                StatusBadge(entry.status.uppercase(), if (entry.status == "completed") DLSuccess else if (entry.status == "failed") DLError else DLNeonCyan)
            }
        }
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024 * 1024))} GB"
    }
}
