package com.jnetaol.droplan.ui.screens

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.*
import com.jnetaol.droplan.data.db.AppDatabase
import com.jnetaol.droplan.data.model.PeerDevice
import com.jnetaol.droplan.data.model.TransferHistory
import com.jnetaol.droplan.engine.*
import com.jnetaol.droplan.logger.DebugLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val networkDiscovery = NetworkDiscovery(application)

    private val _transferHistory = MutableStateFlow<List<TransferHistory>>(emptyList())
    val transferHistory: StateFlow<List<TransferHistory>> = _transferHistory.asStateFlow()

    private val _knownDevices = MutableStateFlow<List<PeerDevice>>(emptyList())
    val knownDevices: StateFlow<List<PeerDevice>> = _knownDevices.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _sendProgressList = MutableStateFlow<List<FileTransferProgress>>(emptyList())
    val sendProgressList: StateFlow<List<FileTransferProgress>> = _sendProgressList.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _clipboardText = MutableStateFlow("")
    val clipboardText: StateFlow<String> = _clipboardText.asStateFlow()

    val versionName = "1.0.2"
    val versionCode = 1

    init {
        DebugLogger.d("AppViewModel", "ViewModel init", "DL-VM-001")
        loadData()
        startServer()
        scope.launch {
            networkDiscovery.transfers.collect { _sendProgressList.value = it }
        }
        scope.launch {
            networkDiscovery.clipboardContent.collect { _clipboardText.value = it }
        }
        loadClipboard()
    }

    private fun loadClipboard() {
        try {
            val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = cm?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                _clipboardText.value = clip.getItemAt(0).text?.toString() ?: ""
            }
        } catch (_: Exception) {}
    }

    private fun loadData() {
        scope.launch {
            try {
                db.transferHistoryDao().getAll().collect { _transferHistory.value = it }
            } catch (e: Exception) {
                DebugLogger.e("AppViewModel", "Load history failed", "DL-ERR-VM-001", e)
            }
        }
        scope.launch {
            try {
                db.peerDeviceDao().getAll().collect { _knownDevices.value = it }
            } catch (e: Exception) {
                DebugLogger.e("AppViewModel", "Load devices failed", "DL-ERR-VM-002", e)
            }
        }
    }

    fun startServer() {
        if (_isServerRunning.value) return
        networkDiscovery.startServer()
        _isServerRunning.value = true
        DebugLogger.i("AppViewModel", "Server started", "DL-VM-002")
    }

    fun stopServer() {
        networkDiscovery.stopServer()
        _isServerRunning.value = false
    }

    fun startScan() {
        networkDiscovery.startScan()
    }

    fun sendFileToPeer(peer: DiscoveredDevice, fileUri: Uri) {
        scope.launch {
            try {
                val context = getApplication<Application>()
                val contentResolver = context.contentResolver
                val fileName = getFileName(fileUri) ?: "unknown_file"
                val mimeType = contentResolver.getType(fileUri) ?: "application/octet-stream"
                val fileData = contentResolver.openInputStream(fileUri)?.readBytes() ?: return@launch

                networkDiscovery.sendFile(peer, fileName, fileData, mimeType)

                val history = TransferHistory(
                    fileName = fileName,
                    fileSize = fileData.size.toLong(),
                    direction = "send",
                    peerName = peer.name,
                    peerIp = peer.ipAddress,
                    status = "completed",
                    progress = 100,
                    mimeType = mimeType
                )
                db.transferHistoryDao().insert(history)
                DebugLogger.i("AppViewModel", "File sent to ${peer.name}", "DL-VM-003", mapOf("file" to fileName))
            } catch (e: Exception) {
                DebugLogger.e("AppViewModel", "Send file failed", "DL-ERR-VM-003", e)
                showToast("Failed to send file: ${e.message}")
            }
        }
    }

    fun sendClipboardToPeer(peer: DiscoveredDevice) {
        val text = _clipboardText.value
        if (text.isBlank()) {
            showToast("Clipboard is empty")
            return
        }
        networkDiscovery.sendClipboard(peer, text)
        showToast("Clipboard sent to ${peer.name}")
    }

    fun requestClipboardFromPeer(peer: DiscoveredDevice) {
        networkDiscovery.requestClipboard(peer)
        showToast("Requesting clipboard from ${peer.name}...")
    }

    fun copyToClipboard(text: String) {
        scope.launch {
            try {
                val context = getApplication<Application>()
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("DropLAN", text)
                cm.setPrimaryClip(clip)
                _clipboardText.value = text
                showToast("Copied to clipboard")
                DebugLogger.d("AppViewModel", "Clipboard updated", "DL-VM-004")
            } catch (e: Exception) {
                DebugLogger.e("AppViewModel", "Clipboard set failed", "DL-ERR-VM-004", e)
            }
        }
    }

    fun addKnownDevice(device: PeerDevice) {
        scope.launch {
            try {
                db.peerDeviceDao().insert(device)
                DebugLogger.d("AppViewModel", "Device saved: ${device.name}", "DL-VM-005")
            } catch (e: Exception) {
                DebugLogger.e("AppViewModel", "Save device failed", "DL-ERR-VM-005", e)
            }
        }
    }

    fun setDeviceTrusted(deviceId: String, trusted: Boolean) {
        scope.launch {
            try {
                db.peerDeviceDao().setTrusted(deviceId, trusted)
            } catch (e: Exception) {
                DebugLogger.e("AppViewModel", "Trust update failed", "DL-ERR-VM-006", e)
            }
        }
    }

    fun deleteDevice(deviceId: String) {
        scope.launch {
            try {
                db.peerDeviceDao().delete(deviceId)
                showToast("Device removed")
            } catch (e: Exception) {
                DebugLogger.e("AppViewModel", "Delete device failed", "DL-ERR-VM-007", e)
            }
        }
    }

    fun clearHistory() {
        scope.launch {
            try {
                db.transferHistoryDao().deleteAll()
                showToast("History cleared")
            } catch (e: Exception) {
                DebugLogger.e("AppViewModel", "Clear history failed", "DL-ERR-VM-008", e)
            }
        }
    }

    fun shareApp() {
        val context = getApplication<Application>()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "DropLAN - Local Network File Drop")
            putExtra(Intent.EXTRA_TEXT, "Try DropLAN for fast local network file transfers! Like AirDrop for Android. Made by jnetai.com")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }

    fun openWebsite() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://jnetai.com")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        getApplication<Application>().startActivity(intent)
    }

    private fun getFileName(uri: Uri): String? {
        val context = getApplication<Application>()
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        if (name == null) {
            name = uri.path?.let { File(it).name }
        }
        return name
    }

    fun showToast(msg: String) { scope.launch { _toastMessage.emit(msg) } }

    override fun onCleared() {
        super.onCleared()
        stopServer()
        scope.cancel()
        DebugLogger.d("AppViewModel", "Cleared", "DL-VM-006")
    }
}
