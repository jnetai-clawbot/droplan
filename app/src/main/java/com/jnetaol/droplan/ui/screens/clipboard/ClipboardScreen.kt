package com.jnetaol.droplan.ui.screens.clipboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnetaol.droplan.engine.DiscoveredDevice
import com.jnetaol.droplan.ui.components.*
import com.jnetaol.droplan.ui.screens.AppViewModel
import com.jnetaol.droplan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val clipboardText by viewModel.clipboardText.collectAsState()
    val discoveredDevices by viewModel.networkDiscovery.discoveredDevices.collectAsState()
    val knownDevices by viewModel.knownDevices.collectAsState()
    var clipInput by remember { mutableStateOf(clipboardText) }
    var selectedPeer by remember { mutableStateOf<DiscoveredDevice?>(null) }

    LaunchedEffect(clipboardText) { clipInput = clipboardText }

    Column(Modifier.fillMaxSize().background(DLBackground)) {
        TopAppBar(
            title = { Text("Clipboard Sync", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = DLTextPrimary) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DLBackground, titleContentColor = DLTextPrimary)
        )

        // Clipboard text area
        NeonCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(180.dp), borderColor = DLNeonTeal.copy(alpha = 0.4f)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Clipboard Content", color = DLTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row {
                        IconButton(onClick = { viewModel.copyToClipboard(clipInput) }, Modifier.size(32.dp)) {
                            Icon(Icons.Default.ContentCopy, "Copy", tint = DLNeonTeal, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = {
                            try {
                                val cm = viewModel.getApplication<android.app.Application>().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = cm.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    clipInput = text
                                    viewModel.copyToClipboard(text)
                                }
                            } catch (_: Exception) {}
                        }, Modifier.size(32.dp)) {
                            Icon(Icons.Default.ContentPaste, "Paste", tint = DLNeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = clipInput, onValueChange = { clipInput = it },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    textStyle = LocalTextStyle.current.copy(color = DLTextPrimary, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DLTextPrimary, unfocusedTextColor = DLTextPrimary,
                        focusedBorderColor = DLNeonTeal.copy(alpha = 0.5f), unfocusedBorderColor = DLSurfaceVariant,
                        cursorColor = DLNeonTeal
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Device selection for sync
        SectionHeader("Sync with Device")
        val allDevices = discoveredDevices.ifEmpty {
            knownDevices.map { DiscoveredDevice(it.deviceId, it.name, it.ipAddress, it.port, it.osType) }
        }

        if (allDevices.isNotEmpty()) {
            LazyColumn(Modifier.height(160.dp).padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allDevices) { device ->
                    FilterChip(
                        selected = selectedPeer?.deviceId == device.deviceId,
                        onClick = { selectedPeer = device },
                        label = { Text(device.name, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Devices, null, Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DLNeonTeal.copy(alpha = 0.2f),
                            containerColor = DLCard
                        )
                    )
                }
            }
        } else {
            EmptyState(Icons.Default.DevicesOther, "No Devices Found", "Start a scan on the home screen to discover devices")
        }

        Spacer(Modifier.height(16.dp))

        // Sync buttons
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlowButton(
                text = "Send Clipboard",
                icon = Icons.Default.Send,
                onClick = { selectedPeer?.let { viewModel.sendClipboardToPeer(it) } },
                glowColor = DLNeonTeal,
                enabled = selectedPeer != null && clipInput.isNotBlank(),
                modifier = Modifier.weight(1f)
            )
            GlowButton(
                text = "Request Clipboard",
                icon = Icons.Default.Download,
                onClick = { selectedPeer?.let { viewModel.requestClipboardFromPeer(it) } },
                glowColor = DLNeonCyan,
                enabled = selectedPeer != null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
