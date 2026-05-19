package com.jnetaol.droplan.engine

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.MulticastLock
import com.jnetaol.droplan.data.model.PeerDevice
import com.jnetaol.droplan.logger.DebugLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.net.*
import java.util.UUID

data class DiscoveredDevice(
    val deviceId: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val osType: String = "android"
)

data class FileTransferItem(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val fileSize: Long,
    val direction: String,
    val peerName: String,
    val mimeType: String = ""
)

data class FileTransferProgress(
    val transferId: String,
    val fileName: String,
    val progress: Int = 0,
    val status: String = "pending",
    val speedKbps: Long = 0,
    val direction: String = "send",
    val peerName: String = ""
)

class NetworkDiscovery(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _transfers = MutableStateFlow<List<FileTransferProgress>>(emptyList())
    val transfers: StateFlow<List<FileTransferProgress>> = _transfers.asStateFlow()

    private val _clipboardContent = MutableStateFlow("")
    val clipboardContent: StateFlow<String> = _clipboardContent.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var multicastLock: MulticastLock? = null
    private val localPort = 21567
    private val discoveryPort = 21568
    private val multicastGroup = "224.0.0.215"
    private val deviceId = UUID.randomUUID().toString()
    private val deviceName = android.os.Build.MODEL ?: "Android Device"

    init {
        DebugLogger.i("NetworkDiscovery", "Initialized", "DL-NET-001", mapOf(
            "deviceId" to deviceId, "port" to localPort.toString()))
        acquireMulticastLock()
    }

    private fun acquireMulticastLock() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            multicastLock = wifiManager.createMulticastLock("DropLAN-Discovery").apply {
                setReferenceCounted(true)
                acquire()
            }
            DebugLogger.d("NetworkDiscovery", "Multicast lock acquired", "DL-NET-002")
        } catch (e: Exception) {
            DebugLogger.e("NetworkDiscovery", "Multicast lock failed", "DL-ERR-NET-001", e)
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val netInt = interfaces.nextElement()
                val addresses = netInt.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "0.0.0.0"
                    }
                }
            }
        } catch (e: Exception) {
            DebugLogger.e("NetworkDiscovery", "Get local IP failed", "DL-ERR-NET-002", e)
        }
        return "0.0.0.0"
    }

    fun startServer() {
        scope.launch {
            try {
                serverSocket?.close()
                serverSocket = ServerSocket(localPort)
                DebugLogger.i("NetworkDiscovery", "Server started on port $localPort", "DL-NET-003")
                while (isActive) {
                    val client = serverSocket?.accept() ?: continue
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    DebugLogger.e("NetworkDiscovery", "Server error", "DL-ERR-NET-003", e)
                }
            }
        }
        startDiscoveryReceiver()
    }

    private suspend fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)
            val request = reader.readLine() ?: return
            DebugLogger.d("NetworkDiscovery", "Received: $request", "DL-NET-005")

            when {
                request.startsWith("DISCOVER") -> {
                    writer.println("DEVICE|$deviceId|$deviceName|$localPort|android")
                }
                request.startsWith("CLIPBOARD") -> {
                    val content = request.removePrefix("CLIPBOARD|")
                    _clipboardContent.value = content
                    writer.println("OK")
                    DebugLogger.d("NetworkDiscovery", "Clipboard received", "DL-NET-006")
                }
                request.startsWith("FILE_INFO") -> {
                    val parts = request.split("|")
                    if (parts.size >= 3) {
                        val fileName = parts[1]
                        val fileSize = parts[2].toLong()
                        val mimeType = parts.getOrNull(3) ?: ""
                        val senderName = parts.getOrNull(4) ?: "Unknown"
                        writer.println("ACCEPT")
                        val transferId = UUID.randomUUID().toString()
                        val progress = FileTransferProgress(transferId, fileName, 0, "receiving", "receive", senderName)
                        _transfers.update { it + progress }
                        receiveFile(socket, fileName, fileSize, transferId)
                    }
                }
                request.startsWith("GET_CLIPBOARD") -> {
                    writer.println("CLIPBOARD|${_clipboardContent.value}")
                }
            }
        } catch (e: Exception) {
            DebugLogger.e("NetworkDiscovery", "Client handler error", "DL-ERR-NET-004", e)
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private suspend fun receiveFile(socket: Socket, fileName: String, fileSize: Long, transferId: String) {
        try {
            val filesDir = context.filesDir
            val receivedDir = File(filesDir, "received")
            if (!receivedDir.exists()) receivedDir.mkdirs()
            val outFile = File(receivedDir, fileName)
            val input = socket.getInputStream()
            val output = FileOutputStream(outFile)
            val buffer = ByteArray(8192)
            var totalRead = 0L
            var bytesRead: Int
            val startTime = System.currentTimeMillis()

            while (input.read(buffer).also { bytesRead = it } != -1 && totalRead < fileSize) {
                output.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                val progress = if (fileSize > 0) ((totalRead * 100) / fileSize).toInt() else 0
                val elapsed = System.currentTimeMillis() - startTime
                val speed = if (elapsed > 0) (totalRead / 1024) / (elapsed / 1000 + 1) else 0
                _transfers.update { list ->
                    list.map { if (it.transferId == transferId) it.copy(progress = progress, speedKbps = speed) else it }
                }
            }
            output.close()
            _transfers.update { list ->
                list.map { if (it.transferId == transferId) it.copy(progress = 100, status = "completed") else it }
            }
            DebugLogger.i("NetworkDiscovery", "File received: $fileName", "DL-NET-007", mapOf("size" to totalRead.toString()))
        } catch (e: Exception) {
            _transfers.update { list ->
                list.map { if (it.transferId == transferId) it.copy(status = "failed") else it }
            }
            DebugLogger.e("NetworkDiscovery", "Receive failed", "DL-ERR-NET-005", e)
        }
    }

    fun sendFile(peer: DiscoveredDevice, fileName: String, fileData: ByteArray, mimeType: String = "") {
        scope.launch {
            try {
                val transferId = UUID.randomUUID().toString()
                val progress = FileTransferProgress(transferId, fileName, 0, "sending", "send", peer.name)
                _transfers.update { it + progress }
                val socket = Socket(peer.ipAddress, peer.port)
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer.println("FILE_INFO|$fileName|${fileData.size}|$mimeType|$deviceName")
                val response = reader.readLine()
                if (response != "ACCEPT") {
                    _transfers.update { list -> list.map { if (it.transferId == transferId) it.copy(status = "rejected") else it } }
                    socket.close()
                    return@launch
                }
                val output = socket.getOutputStream()
                val startTime = System.currentTimeMillis()
                var sent = 0
                val chunkSize = 8192
                while (sent < fileData.size) {
                    val end = minOf(sent + chunkSize, fileData.size)
                    output.write(fileData, sent, end - sent)
                    sent = end
                    val elapsed = System.currentTimeMillis() - startTime
                    val speed = if (elapsed > 0) (sent / 1024) / (elapsed / 1000 + 1) else 0
                    _transfers.update { list ->
                        list.map { if (it.transferId == transferId) it.copy(progress = (sent * 100) / fileData.size, speedKbps = speed) else it }
                    }
                }
                output.flush()
                socket.close()
                _transfers.update { list -> list.map { if (it.transferId == transferId) it.copy(progress = 100, status = "completed") else it } }
                DebugLogger.i("NetworkDiscovery", "File sent: $fileName", "DL-NET-008", mapOf("size" to fileData.size.toString()))
            } catch (e: Exception) {
                DebugLogger.e("NetworkDiscovery", "Send failed", "DL-ERR-NET-006", e)
            }
        }
    }

    fun sendClipboard(peer: DiscoveredDevice, content: String) {
        scope.launch {
            try {
                val socket = Socket(peer.ipAddress, peer.port)
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer.println("CLIPBOARD|$content")
                reader.readLine()
                socket.close()
                DebugLogger.d("NetworkDiscovery", "Clipboard sent to ${peer.name}", "DL-NET-009")
            } catch (e: Exception) {
                DebugLogger.e("NetworkDiscovery", "Clipboard send failed", "DL-ERR-NET-007", e)
            }
        }
    }

    fun requestClipboard(peer: DiscoveredDevice) {
        scope.launch {
            try {
                val socket = Socket(peer.ipAddress, peer.port)
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer.println("GET_CLIPBOARD")
                val response = reader.readLine() ?: return@launch
                if (response.startsWith("CLIPBOARD|")) {
                    _clipboardContent.value = response.removePrefix("CLIPBOARD|")
                }
                socket.close()
                DebugLogger.d("NetworkDiscovery", "Clipboard retrieved from ${peer.name}", "DL-NET-010")
            } catch (e: Exception) {
                DebugLogger.e("NetworkDiscovery", "Clipboard retrieve failed", "DL-ERR-NET-008", e)
            }
        }
    }

    fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        _discoveredDevices.value = emptyList()
        DebugLogger.d("NetworkDiscovery", "Scan started", "DL-NET-011")
        scope.launch {
            try {
                val socket = DatagramSocket().apply { broadcast = true }
                val buffer = "DISCOVER".toByteArray()
                val packet = DatagramPacket(buffer, buffer.size, InetAddress.getByName(multicastGroup), discoveryPort)
                socket.send(packet)
                socket.close()
            } catch (e: Exception) {
                DebugLogger.e("NetworkDiscovery", "Discovery send failed", "DL-ERR-NET-009", e)
            }
        }
        scope.launch {
            delay(5000)
            _isScanning.value = false
            if (_discoveredDevices.value.isEmpty()) {
                DebugLogger.d("NetworkDiscovery", "Scan complete: no devices found", "DL-NET-012")
            }
        }
    }

    private fun startDiscoveryReceiver() {
        scope.launch {
            try {
                val socket = MulticastSocket(discoveryPort).apply {
                    joinGroup(InetAddress.getByName(multicastGroup))
                }
                DebugLogger.d("NetworkDiscovery", "Discovery receiver started", "DL-NET-013")
                while (isActive) {
                    val buffer = ByteArray(4096)
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length)
                    val senderIp = packet.address.hostAddress ?: continue
                    if (senderIp == getLocalIpAddress()) continue
                    if (message.startsWith("DISCOVER")) {
                        respondToDiscovery(senderIp)
                    } else if (message.startsWith("DEVICE|")) {
                        val parts = message.split("|")
                        if (parts.size >= 5) {
                            val device = DiscoveredDevice(parts[1], parts[2], senderIp, parts[3].toInt(), parts[4])
                            _discoveredDevices.update { list ->
                                if (list.none { it.deviceId == device.deviceId }) list + device else list
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    DebugLogger.e("NetworkDiscovery", "Receiver error", "DL-ERR-NET-010", e)
                }
            }
        }
    }

    private suspend fun respondToDiscovery(requesterIp: String) {
        try {
            val socket = DatagramSocket()
            val response = "DEVICE|$deviceId|$deviceName|$localPort|android@${android.os.Build.MODEL}"
            val data = response.toByteArray()
            val packet = DatagramPacket(data, data.size, InetAddress.getByName(requesterIp), discoveryPort)
            socket.send(packet)
            socket.close()
        } catch (e: Exception) {
            DebugLogger.e("NetworkDiscovery", "Discovery response failed", "DL-ERR-NET-011", e)
        }
    }

    fun stopServer() {
        scope.cancel()
        serverSocket?.close()
        multicastLock?.release()
        DebugLogger.i("NetworkDiscovery", "Server stopped", "DL-NET-014")
    }
}
