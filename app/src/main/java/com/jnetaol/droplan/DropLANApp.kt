package com.jnetaol.droplan

import android.app.Application
import com.jnetaol.droplan.logger.DebugLogger

class DropLANApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DebugLogger.init(this)
        DebugLogger.i("DropLANApp", "App started", "DL-APP-001", mapOf("version" to "1.0.1"))
    }
    override fun onTerminate() {
        DebugLogger.i("DropLANApp", "Terminating", "DL-APP-002")
        DebugLogger.shutdown()
        super.onTerminate()
    }
}
