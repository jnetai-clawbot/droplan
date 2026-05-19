package com.jnetaol.droplan.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfer_history")
data class TransferHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "file_size") val fileSize: Long = 0,
    @ColumnInfo(name = "direction") val direction: String = "send",
    @ColumnInfo(name = "peer_name") val peerName: String = "",
    @ColumnInfo(name = "peer_ip") val peerIp: String = "",
    @ColumnInfo(name = "status") val status: String = "completed",
    @ColumnInfo(name = "progress") val progress: Int = 100,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "mime_type") val mimeType: String = ""
)

@Entity(tableName = "known_devices")
data class PeerDevice(
    @PrimaryKey @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "name") val name: String = "",
    @ColumnInfo(name = "ip_address") val ipAddress: String = "",
    @ColumnInfo(name = "port") val port: Int = 0,
    @ColumnInfo(name = "last_seen") val lastSeen: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_trusted") val isTrusted: Boolean = false,
    @ColumnInfo(name = "os_type") val osType: String = ""
)
