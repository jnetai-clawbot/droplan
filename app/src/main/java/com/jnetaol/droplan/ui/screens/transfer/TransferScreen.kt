package com.jnetaol.droplan.ui.screens.transfer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnetaol.droplan.engine.DiscoveredDevice
import com.jnetaol.droplan.ui.components.*
import com.jnetaol.droplan.ui.screens.AppViewModel
import com.jnetaol.droplan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val discoveredDevices by viewModel.networkDiscovery.discoveredDevices.collectAsState()
    val transfers by viewModel.sendProgressList.collectAsState()
    val history by viewModel.transferHistory.collectAsState()
    var selectedDevice by remember { mutableStateOf<DiscoveredDevice?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var selectedFiles by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> selectedFiles = uris }

    Column(Modifier.fillMaxSize().background(DLBackground)) {
        TopAppBar(
            title = { Text("File Transfer", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = DLTextPrimary) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DLBackground, titleContentColor = DLTextPrimary)
        )

        // Device selector
        if (discoveredDevices.isNotEmpty()) {
            SectionHeader("Select Device")
            LazyColumn(Modifier.height(120.dp).padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(discoveredDevices) { device ->
                    FilterChip(
                        selected = selectedDevice?.deviceId == device.deviceId,
                        onClick = { selectedDevice = device },
                        label = { Text(device.name, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Devices, null, Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DLPrimary.copy(alpha = 0.2f),
                            containerColor = DLCard
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Drag-drop area
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(160.dp).clip(RoundedCornerShape(16.dp))
            .background(if (isDragging) DLPrimary.copy(alpha = 0.1f) else DLSurfaceVariant.copy(alpha = 0.3f))
            .border(2.dp, if (isDragging) DLPrimary else DLTextMuted.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.UploadFile, null, Modifier.size(48.dp), tint = if (isDragging) DLNeonCyan else DLTextMuted)
                Spacer(Modifier.height(12.dp))
                Text("Tap to Select Files", color = DLTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("or drag & drop files here", color = DLTextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                GlowButton(text = "Browse Files", icon = Icons.Default.FolderOpen, onClick = { filePickerLauncher.launch(arrayOf("*/*")) }, glowColor = DLNeonCyan)
            }
        }

        // Selected files list
        if (selectedFiles.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SectionHeader("Selected Files (${selectedFiles.size})", "Clear") { selectedFiles = emptyList() }
            selectedFiles.take(3).forEach { uri ->
                Text(uri.lastPathSegment ?: "Unknown", Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = DLTextSecondary, fontSize = 13.sp)
            }
            if (selectedFiles.size > 3) {
                Text("... and ${selectedFiles.size - 3} more", Modifier.padding(horizontal = 16.dp),
                    color = DLTextMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                selectedDevice?.let { device ->
                    selectedFiles.forEach { uri -> viewModel.sendFileToPeer(device, uri) }
                    selectedFiles = emptyList()
                    viewModel.showToast("Sending ${selectedFiles.size} file(s) to ${device.name}")
                }
            }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                enabled = selectedDevice != null,
                colors = ButtonDefaults.buttonColors(containerColor = DLPrimary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Send, null, Modifier.size(20.dp), tint = DLNeonCyan)
                Spacer(Modifier.width(8.dp))
                Text(if (selectedDevice != null) "Send to ${selectedDevice!!.name}" else "Select a device first", color = DLNeonCyan, fontSize = 14.sp)
            }
        }

        // Active transfers
        if (transfers.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SectionHeader("Active Transfers")
            LazyColumn(Modifier.weight(0.4f), contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transfers, key = { it.transferId }) { transfer -> TransferProgressCard(transfer) }
            }
        }

        // Transfer history
        if (history.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SectionHeader("Recent Transfers", "Clear") { viewModel.clearHistory() }
            LazyColumn(Modifier.weight(0.6f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(history.take(20), key = { it.id }) { item -> HistoryItem(item) }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }

        if (transfers.isEmpty() && history.isEmpty()) {
            EmptyState(Icons.Default.SwapHoriz, "No Transfers Yet", "Select files and a device to start transferring")
        }
    }
}
