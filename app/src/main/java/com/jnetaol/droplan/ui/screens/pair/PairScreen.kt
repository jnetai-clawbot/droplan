package com.jnetaol.droplan.ui.screens.pair

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnetaol.droplan.engine.PairInfo
import com.jnetaol.droplan.engine.QRCodeHandler
import com.jnetaol.droplan.data.model.PeerDevice
import com.jnetaol.droplan.ui.components.*
import com.jnetaol.droplan.ui.screens.AppViewModel
import com.jnetaol.droplan.ui.theme.*
import java.net.Inet4Address
import java.net.NetworkInterface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showScanner by remember { mutableStateOf(false) }
    var scannedResult by remember { mutableStateOf<String?>(null) }
    var pairStatus by remember { mutableStateOf("") }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showScanner = true
        } else {
            pairStatus = "Camera permission required for QR scanning"
        }
    }

    LaunchedEffect(Unit) {
        val localIp = getLocalIp()
        val qrInfo = PairInfo(
            deviceId = java.util.UUID.randomUUID().toString().take(8),
            deviceName = android.os.Build.MODEL ?: "Android",
            ipAddress = localIp,
            port = 21567,
            osType = "android"
        )
        qrBitmap = QRCodeHandler.generatePairQR(qrInfo)
    }

    Column(Modifier.fillMaxSize().background(DLBackground)) {
        TopAppBar(
            title = { Text("QR Pairing", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = DLTextPrimary) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DLBackground, titleContentColor = DLTextPrimary)
        )

        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Scan this QR code from another DropLAN device to pair instantly",
                color = DLTextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)

            Spacer(Modifier.height(20.dp))

            // QR Code display
            Card(Modifier.size(260.dp), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                    qrBitmap?.let {
                        Image(it.asImageBitmap(), "Pair QR Code", Modifier.fillMaxSize())
                    } ?: CircularProgressIndicator(color = DLPrimary)
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("My IP: ${getLocalIp()}", color = DLNeonCyan, fontSize = 15.sp, fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(20.dp))

            Divider(color = DLSurfaceVariant, thickness = 1.dp)
            Spacer(Modifier.height(20.dp))

            Text("--- OR ---", color = DLTextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))

            GlowButton(
                text = if (showScanner) "Scanning..." else "Scan QR Code",
                icon = Icons.Default.QrCodeScanner,
                onClick = {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                glowColor = DLNeonCyan
            )

            if (showScanner) {
                Spacer(Modifier.height(16.dp))

                // Simulated scanner UI (real CameraX integration would be needed for production)
                Card(Modifier.fillMaxWidth().height(200.dp), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, null, Modifier.size(48.dp), tint = DLNeonCyan.copy(alpha = 0.5f))
                            Spacer(Modifier.height(12.dp))
                            Text("Camera Preview", color = DLTextSecondary, fontSize = 14.sp)
                            Text("Point at QR code to scan", color = DLTextMuted, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            TextButton(onClick = {
                                // Simulate a QR scan for demo purposes
                                val demoResult = "DROPLAN|device123|Pixel 7|192.168.1.100|21567|android"
                                scannedResult = demoResult
                                val pairInfo = QRCodeHandler.parsePairQR(demoResult)
                                if (pairInfo != null) {
                                    viewModel.addKnownDevice(PeerDevice(
                                        deviceId = pairInfo.deviceId,
                                        name = pairInfo.deviceName,
                                        ipAddress = pairInfo.ipAddress,
                                        port = pairInfo.port,
                                        osType = pairInfo.osType,
                                        isTrusted = true
                                    ))
                                    pairStatus = "Paired with ${pairInfo.deviceName} successfully!"
                                    showScanner = false
                                } else {
                                    pairStatus = "Invalid QR code format"
                                }
                            }) { Text("Simulate Scan (Demo)", color = DLNeonCyan, fontSize = 13.sp) }
                        }
                    }
                }
            }

            // Manual entry
            Spacer(Modifier.height(20.dp))
            Divider(color = DLSurfaceVariant, thickness = 1.dp)
            Spacer(Modifier.height(16.dp))
            Text("Manual Pair", color = DLTextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            var manualIp by remember { mutableStateOf("") }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = manualIp, onValueChange = { manualIp = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Device IP:Port", color = DLTextMuted) },
                    textStyle = LocalTextStyle.current.copy(color = DLTextPrimary, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DLTextPrimary, unfocusedTextColor = DLTextPrimary,
                        focusedBorderColor = DLPrimary.copy(alpha = 0.5f), unfocusedBorderColor = DLSurfaceVariant,
                        cursorColor = DLPrimary
                    ),
                    shape = RoundedCornerShape(8.dp), singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    if (manualIp.isNotBlank()) {
                        val parts = manualIp.split(":")
                        val ip = parts[0]
                        val port = parts.getOrNull(1)?.toIntOrNull() ?: 21567
                        viewModel.addKnownDevice(PeerDevice(
                            deviceId = "manual_${System.currentTimeMillis()}",
                            name = "Manual Device",
                            ipAddress = ip, port = port, osType = "unknown", isTrusted = true
                        ))
                        pairStatus = "Manual device added: $ip:$port"
                        manualIp = ""
                    }
                }, enabled = manualIp.isNotBlank()) {
                    Icon(Icons.Default.AddCircle, "Add", tint = if (manualIp.isNotBlank()) DLNeonCyan else DLTextMuted)
                }
            }

            // Status
            if (pairStatus.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                NeonCard(Modifier.fillMaxWidth(), borderColor = if (pairStatus.contains("successfully")) DLSuccess.copy(alpha = 0.5f) else DLNeonCyan.copy(alpha = 0.3f)) {
                    Text(pairStatus, Modifier.padding(12.dp), color = DLTextPrimary, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

private fun getLocalIp(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (!address.isLoopbackAddress && address is Inet4Address) {
                    return address.hostAddress ?: "0.0.0.0"
                }
            }
        }
    } catch (_: Exception) {}
    return "0.0.0.0"
}
