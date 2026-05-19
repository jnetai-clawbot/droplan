package com.jnetaol.droplan.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnetaol.droplan.ui.components.*
import com.jnetaol.droplan.ui.screens.AppViewModel
import com.jnetaol.droplan.ui.theme.*
import com.jnetaol.droplan.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isServerRunning by viewModel.isServerRunning.collectAsState()

    Column(Modifier.fillMaxSize().background(DLBackground)) {
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = DLTextPrimary) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DLBackground, titleContentColor = DLTextPrimary)
        )

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

            // App Info
            NeonCard(Modifier.fillMaxWidth(), borderColor = DLPrimary.copy(alpha = 0.3f)) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Wifi, null, Modifier.size(48.dp), tint = DLNeonCyan)
                    Spacer(Modifier.height(12.dp))
                    Text("DropLAN", color = DLTextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Local Network File Drop", color = DLTextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Version ${viewModel.versionName} (${viewModel.versionCode})", color = DLTextMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Build: ${BuildConfig.BUILD_TYPE}", color = DLTextMuted, fontSize = 12.sp)

                    Spacer(Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { viewModel.openWebsite() }) {
                            Text("Made By jnetaol.com", color = DLNeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Server Controls
            SectionHeader("Server")
            NeonCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, null, Modifier.size(20.dp), tint = if (isServerRunning) DLSuccess else DLTextMuted)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("File Server", color = DLTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(if (isServerRunning) "Accepting connections" else "Server stopped", color = DLTextMuted, fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = isServerRunning,
                            onCheckedChange = { if (it) viewModel.startServer() else viewModel.stopServer() },
                            colors = SwitchDefaults.colors(checkedThumbColor = DLSuccess, checkedTrackColor = DLSuccess.copy(alpha = 0.3f))
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Data Management
            SectionHeader("Data")
            NeonCard(Modifier.fillMaxWidth()) {
                Column {
                    SettingsRow(Icons.Default.DeleteSweep, "Clear Transfer History", "Remove all past transfer records") {
                        viewModel.clearHistory()
                    }
                    Divider(color = DLSurfaceVariant, thickness = 0.5.dp)
                    SettingsRow(Icons.Default.FolderDelete, "Clear Known Devices", "Remove all paired devices") {
                        viewModel.showToast("Devices cleared")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Actions
            SectionHeader("Actions")
            NeonCard(Modifier.fillMaxWidth()) {
                Column {
                    SettingsRow(Icons.Default.Share, "Share DropLAN", "Tell friends about DropLAN") {
                        viewModel.shareApp()
                    }
                    Divider(color = DLSurfaceVariant, thickness = 0.5.dp)
                    SettingsRow(Icons.Default.Update, "Check For Updates", "Version ${viewModel.versionName}") {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/jnetaol/droplan/releases"))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                    Divider(color = DLSurfaceVariant, thickness = 0.5.dp)
                    SettingsRow(Icons.Default.Info, "About DropLAN", "Like AirDrop, for Android & Linux") {
                        viewModel.openWebsite()
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Network Info
            SectionHeader("Network Info")
            NeonCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    InfoRow("Port", "21567")
                    InfoRow("Discovery", "UDP Multicast 224.0.0.215:21568")
                    InfoRow("Protocol", "TCP (files) + UDP (discovery)")
                    InfoRow("LAN Only", "Yes - no internet needed")
                }
            }

            Spacer(Modifier.height(24.dp))

            // Footer
            Text(
                "DropLAN v${viewModel.versionName}  |  Made by jnetaol.com",
                Modifier.fillMaxWidth(),
                color = DLTextMuted, fontSize = 11.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Local network file transfers. No cloud, no accounts, just your network.",
                Modifier.fillMaxWidth(),
                color = DLTextMuted, fontSize = 11.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = DLNeonCyan)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = DLTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = DLTextMuted, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = DLTextMuted)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = DLTextSecondary, fontSize = 13.sp)
        Text(value, color = DLNeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
