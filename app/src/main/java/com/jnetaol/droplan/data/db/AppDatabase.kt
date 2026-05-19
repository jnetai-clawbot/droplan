package com.jnetaol.droplan.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jnetaol.droplan.data.model.PeerDevice
import com.jnetaol.droplan.data.model.TransferHistory
import com.jnetaol.droplan.logger.DebugLogger

@Database(entities = [TransferHistory::class, PeerDevice::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transferHistoryDao(): TransferHistoryDao
    abstract fun peerDeviceDao(): PeerDeviceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }
        private fun buildDatabase(context: Context): AppDatabase = try {
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "droplan.db")
                .fallbackToDestructiveMigration().build()
        } catch (e: Exception) {
            DebugLogger.e("AppDatabase", "DB creation failed", "DL-DB-001", e)
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "droplan_fallback.db")
                .fallbackToDestructiveMigration().build()
        }
    }
}
