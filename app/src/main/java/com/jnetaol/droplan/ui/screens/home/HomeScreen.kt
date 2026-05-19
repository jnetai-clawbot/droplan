package com.jnetaol.droplan.ui.screens.home

import androidx.compose.animation.*
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
import com.jnetaol.droplan.data.model.PeerDevice
import com.jnetaol.droplan.engine.DiscoveredDevice
import com.jnetaol.droplan.ui.components.*
import com.jnetaol.droplan.ui.screens.AppViewModel
import com.jnetaol.droplan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToTransfer: () -> Unit,
    onNavigateToClipboard: () -> Unit,
    onNavigateToPair: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val discoveredDevices by viewModel.networkDiscovery.discoveredDevices.collectAsState()
    val isScanning by viewModel.networkDiscovery.isScanning.collectAsState()
    val isServerRunning by viewModel.isServerRunning.collectAsState()
    val knownDevices by viewModel.knownDevices.collectAsState()
    val toastMessages = viewModel.toastMessage

    LaunchedEffect(Unit) {
        toastMessages.collect { /* handled by snackbar if needed */ }
    }

    Column(Modifier.fillMaxSize().background(DLBackground)) {
        Row(Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("DropLAN", color = DLTextWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Local Network File Drop", color = DLTextMuted, fontSize = 13.sp)
            }
            Row {
                IconButton(onNavigateToPair) { Icon(Icons.Default.QrCodeScanner, "Pair", tint = DLNeonCyan) }
                IconButton(onNavigateToSettings) { Icon(Icons.Default.Settings, "Settings", tint = DLTextSecondary) }
            }
        }

        // Network status
        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = if (isServerRunning) DLSuccess.copy(alpha = 0.1f) else DLError.copy(alpha = 0.1f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isServerRunning) DLSuccess.copy(alpha = 0.3f) else DLError.copy(alpha = 0.3f))) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, null, tint = if (isServerRunning) DLSuccess else DLError, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (isServerRunning) "Server Active" else "Server Stopped", color = DLTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (isServerRunning) "Listening for connections on LAN" else "Tap scan to discover devices", color = DLTextMuted, fontSize = 12.sp)
                }
                if (isServerRunning) {
                    Button(onClick = { viewModel.stopServer() },
                        colors = ButtonDefaults.buttonColors(containerColor = DLError.copy(alpha = 0.15f)), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)) {
                        Text("Stop", color = DLError, fontSize = 12.sp)
                    }
                } else {
                    Button(onClick = { viewModel.startServer() },
                        colors = ButtonDefaults.buttonColors(containerColor = DLSuccess.copy(alpha = 0.15f)), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)) {
                        Text("Start", color = DLSuccess, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Action buttons
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlowButton(text = "Transfer", icon = Icons.Default.Send, onClick = onNavigateToTransfer, glowColor = DLNeonCyan, modifier = Modifier.weight(1f))
            GlowButton(text = "Clipboard", icon = Icons.Default.ContentCopy, onClick = onNavigateToClipboard, glowColor = DLNeonTeal, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        // Scanning and device list
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(title = "Nearby Devices")
            Button(onClick = { viewModel.startScan() }, enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = DLPrimary.copy(alpha = 0.15f)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)) {
                if (isScanning) {
                    CircularProgressIndicator(Modifier.size(14.dp), color = DLNeonCyan, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isScanning) "Scanning..." else "Scan", color = DLNeonCyan, fontSize = 13.sp)
            }
        }

        if (discoveredDevices.isEmpty() && knownDevices.isEmpty()) {
            EmptyState(Icons.Default.DevicesOther, "No Devices Found", "Make sure both devices are on the same WiFi network\nand DropLAN is running on both")
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(discoveredDevices, key = { it.deviceId }) { device ->
                    DeviceCard(
                        deviceName = device.name,
                        deviceId = device.deviceId,
                        osType = device.osType,
                        isOnline = true,
                        isTrusted = knownDevices.any { it.deviceId == device.deviceId && it.isTrusted },
                        onTap = {
                            viewModel.addKnownDevice(PeerDevice(
                                deviceId = device.deviceId, name = device.name,
                                ipAddress = device.ipAddress, port = device.port,
                                osType = device.osType, isTrusted = false
                            ))
                            onNavigateToTransfer()
                        }
                    )
                }
                if (knownDevices.isNotEmpty()) {
                    item { Spacer(Modifier.height(8.dp)); SectionHeader(title = "Known Devices") }
                    items(knownDevices, key = { it.deviceId }) { device ->
                        val isOnline = discoveredDevices.any { it.deviceId == device.deviceId }
                        DeviceCard(
                            deviceName = device.name,
                            deviceId = device.deviceId,
                            osType = device.osType,
                            isOnline = isOnline,
                            isTrusted = device.isTrusted,
                            onTap = { onNavigateToTransfer() }
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
