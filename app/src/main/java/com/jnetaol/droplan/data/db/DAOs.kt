package com.jnetaol.droplan.data.db

import androidx.room.*
import com.jnetaol.droplan.data.model.PeerDevice
import com.jnetaol.droplan.data.model.TransferHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferHistoryDao {
    @Query("SELECT * FROM transfer_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<TransferHistory>>

    @Query("SELECT * FROM transfer_history WHERE status = :status ORDER BY timestamp DESC")
    fun getByStatus(status: String): Flow<List<TransferHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TransferHistory): Long

    @Update
    suspend fun update(entry: TransferHistory)

    @Query("UPDATE transfer_history SET progress = :progress, status = :status WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Int, status: String)

    @Query("DELETE FROM transfer_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM transfer_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM transfer_history")
    suspend fun getCount(): Int
}

@Dao
interface PeerDeviceDao {
    @Query("SELECT * FROM known_devices ORDER BY last_seen DESC")
    fun getAll(): Flow<List<PeerDevice>>

    @Query("SELECT * FROM known_devices WHERE device_id = :deviceId")
    suspend fun getById(deviceId: String): PeerDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: PeerDevice)

    @Update
    suspend fun update(device: PeerDevice)

    @Query("UPDATE known_devices SET is_trusted = :trusted WHERE device_id = :deviceId")
    suspend fun setTrusted(deviceId: String, trusted: Boolean)

    @Query("DELETE FROM known_devices WHERE device_id = :deviceId")
    suspend fun delete(deviceId: String)

    @Query("DELETE FROM known_devices")
    suspend fun deleteAll()
}
