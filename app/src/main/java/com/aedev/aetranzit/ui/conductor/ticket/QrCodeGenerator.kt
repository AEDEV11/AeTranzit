package com.aedev.aetranzit.ui.conductor.ticket

import android.graphics.Bitmap
import com.aedev.aetranzit.di.DefaultDispatcher
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrCodeGenerator @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    fun encodeBitMatrix(
        payload: String,
        sizePx: Int = DEFAULT_QR_SIZE_PX,
        quietZoneModules: Int = DEFAULT_QUIET_ZONE_MODULES
    ): BitMatrix = createBitMatrix(
        payload = payload,
        sizePx = sizePx,
        quietZoneModules = quietZoneModules
    )

    suspend fun generateQrBitmap(
        payload: String,
        sizePx: Int = DEFAULT_QR_SIZE_PX,
        quietZoneModules: Int = DEFAULT_QUIET_ZONE_MODULES
    ): Bitmap = withContext(defaultDispatcher) {
        renderBitmapSync(
            payload = payload,
            sizePx = sizePx,
            quietZoneModules = quietZoneModules
        )
    }

    companion object {
        const val DEFAULT_QR_SIZE_PX = 512
        const val DEFAULT_QUIET_ZONE_MODULES = 2
        private const val COLOR_BLACK = -0x1000000 // 0xFF000000
        private const val COLOR_WHITE = -0x1 // 0xFFFFFFFF

        fun createBitMatrix(
            payload: String,
            sizePx: Int = DEFAULT_QR_SIZE_PX,
            quietZoneModules: Int = DEFAULT_QUIET_ZONE_MODULES
        ): BitMatrix {
            val cleanPayload = payload.trim()
            require(cleanPayload.isNotEmpty()) { "QR payload must not be blank." }
            require(sizePx > 0) { "QR sizePx must be positive." }
            require(quietZoneModules >= 0) { "QR quietZoneModules must be non-negative." }

            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to quietZoneModules,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            return QRCodeWriter().encode(
                cleanPayload,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
        }

        fun renderBitmapSync(
            payload: String,
            sizePx: Int = DEFAULT_QR_SIZE_PX,
            quietZoneModules: Int = DEFAULT_QUIET_ZONE_MODULES
        ): Bitmap {
            val matrix = createBitMatrix(
                payload = payload,
                sizePx = sizePx,
                quietZoneModules = quietZoneModules
            )
            val width = matrix.width
            val height = matrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val rowOffset = y * width
                for (x in 0 until width) {
                    pixels[rowOffset + x] = if (matrix[x, y]) COLOR_BLACK else COLOR_WHITE
                }
            }
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        }
    }
}
