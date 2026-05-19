package com.jnetaol.droplan.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.jnetaol.droplan.logger.DebugLogger
import java.io.ByteArrayOutputStream

data class PairInfo(
    val deviceId: String,
    val deviceName: String,
    val ipAddress: String,
    val port: Int,
    val osType: String = "android"
)

object QRCodeHandler {

    fun generatePairQR(pairInfo: PairInfo): Bitmap {
        val qrContent = "DROPLAN|${pairInfo.deviceId}|${pairInfo.deviceName}|${pairInfo.ipAddress}|${pairInfo.port}|${pairInfo.osType}"
        DebugLogger.d("QRCodeHandler", "QR generated for ${pairInfo.deviceName}", "DL-QR-001")
        return generateQRBitmap(qrContent, 400)
    }

    fun parsePairQR(qrContent: String): PairInfo? {
        return try {
            if (!qrContent.startsWith("DROPLAN|")) return null
            val parts = qrContent.split("|")
            if (parts.size < 6) return null
            val info = PairInfo(
                deviceId = parts[1],
                deviceName = parts[2],
                ipAddress = parts[3],
                port = parts[4].toInt(),
                osType = parts[5]
            )
            DebugLogger.i("QRCodeHandler", "QR parsed: ${info.deviceName}", "DL-QR-002", mapOf("ip" to info.ipAddress))
            info
        } catch (e: Exception) {
            DebugLogger.e("QRCodeHandler", "QR parse failed", "DL-ERR-QR-001", e)
            null
        }
    }

    private fun generateQRBitmap(content: String, size: Int): Bitmap {
        val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val whitePaint = Paint().apply { this.color = Color.WHITE }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), whitePaint)
        val blackPaint = Paint().apply { this.color = Color.BLACK }
        for (x in 0 until size) {
            for (y in 0 until size) {
                if (bitMatrix[x, y]) {
                    canvas.drawRect(x.toFloat(), y.toFloat(), x.toFloat() + 1f, y.toFloat() + 1f, blackPaint)
                }
            }
        }
        return bitmap
    }
}
