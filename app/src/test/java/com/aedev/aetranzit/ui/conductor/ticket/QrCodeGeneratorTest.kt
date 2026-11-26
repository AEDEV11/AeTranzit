package com.aedev.aetranzit.ui.conductor.ticket

import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeGeneratorTest {

    private val generator = QrCodeGenerator(StandardTestDispatcher())

    @Test
    fun `encodeBitMatrix generates scannable QR matrix with white quiet zone`() {
        val payload =
            "AETRANZIT|v1|F8A1BC23|trip-501|B:AE-108|R:Cubao -> Baguio|S12|Dau Terminal|STUDENT|128.00|1764144000000"

        val matrix = generator.encodeBitMatrix(
            payload = payload,
            sizePx = 256,
            quietZoneModules = 2
        )

        assertTrue(matrix.width >= 256)
        assertEquals(matrix.width, matrix.height)

        // Verify top and bottom border rows are pure quiet zone (false = white)
        for (x in 0 until matrix.width) {
            assertFalse("Top border should be white quiet zone at x=$x", matrix.get(x, 0))
            assertFalse(
                "Bottom border should be white quiet zone at x=$x",
                matrix.get(x, matrix.height - 1)
            )
        }

        // Verify interior contains dark QR modules
        var hasDarkModule = false
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix.get(x, y)) {
                    hasDarkModule = true
                    break
                }
            }
            if (hasDarkModule) break
        }
        assertTrue(hasDarkModule)

        // Round-trip decode using ZXing QRCodeReader to guarantee scanner readability
        val decoded = QRCodeReader().decode(
            BinaryBitmap(HybridBinarizer(BitMatrixLuminanceSource(matrix)))
        )
        assertEquals(payload, decoded.text)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `encodeBitMatrix rejects blank payload`() {
        generator.encodeBitMatrix(payload = "   ", sizePx = 256)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `encodeBitMatrix rejects non-positive size`() {
        generator.encodeBitMatrix(payload = "AETRANZIT|v1|TEST", sizePx = 0)
    }

    private class BitMatrixLuminanceSource(
        private val matrix: BitMatrix
    ) : LuminanceSource(matrix.width, matrix.height) {

        override fun getRow(y: Int, row: ByteArray?): ByteArray {
            val result = if (row == null || row.size < width) ByteArray(width) else row
            for (x in 0 until width) {
                result[x] = if (matrix.get(x, y)) 0.toByte() else 0xFF.toByte()
            }
            return result
        }

        override fun getMatrix(): ByteArray {
            val area = width * height
            val luminances = ByteArray(area)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    luminances[offset + x] = if (matrix.get(x, y)) 0.toByte() else 0xFF.toByte()
                }
            }
            return luminances
        }
    }
}
