package com.aedev.aetranzit.ui.conductor.ticket

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import com.aedev.aetranzit.R
import com.aedev.aetranzit.di.DefaultDispatcher
import com.aedev.aetranzit.ui.theme.BlueContainerLight
import com.aedev.aetranzit.ui.theme.BlueOnContainerLight
import com.aedev.aetranzit.ui.theme.DeepNavy
import com.aedev.aetranzit.ui.theme.ElectricBlue
import com.aedev.aetranzit.ui.theme.LightBackground
import com.aedev.aetranzit.ui.theme.LightOutline
import com.aedev.aetranzit.ui.theme.TealContainerLight
import com.aedev.aetranzit.ui.theme.TealOnContainerLight
import com.aedev.aetranzit.ui.theme.TicketPaperBackground
import com.aedev.aetranzit.ui.theme.TicketPaperDivider
import com.aedev.aetranzit.ui.theme.TicketPaperInk
import com.aedev.aetranzit.ui.theme.TicketPaperSectionBg
import com.aedev.aetranzit.ui.theme.TicketPaperSubtleInk
import com.aedev.aetranzit.ui.theme.TransitTealDark
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class DigitalTicketRenderData(
    val referenceCode: String,
    val statusLabel: String,
    val isDroppedOff: Boolean,
    val seatNumber: String,
    val routeLabel: String,
    val destination: String,
    val busNumber: String,
    val plateNumber: String,
    val passengerTypeLabel: String,
    val isDiscounted: Boolean,
    val fareFormatted: String,
    val cashFormatted: String,
    val changeFormatted: String,
    val conductorName: String,
    val issuedTimestamp: String,
    val qrPayload: String
)

@Singleton
class DigitalTicketBitmapRenderer @Inject constructor(
    private val qrCodeGenerator: QrCodeGenerator,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    suspend fun renderTicketBitmap(
        context: Context,
        data: DigitalTicketRenderData
    ): Bitmap = withContext(defaultDispatcher) {
        val qrBitmap = qrCodeGenerator.generateQrBitmap(
            payload = data.qrPayload,
            sizePx = QR_BITMAP_PX
        )
        val bitmap = createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val outerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LightBackground.toArgb()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(), outerBgPaint)

        val cardRect = RectF(
            CARD_MARGIN,
            CARD_MARGIN,
            CANVAS_WIDTH - CARD_MARGIN,
            CANVAS_HEIGHT - CARD_MARGIN
        )
        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TicketPaperBackground.toArgb()
            style = Paint.Style.FILL
        }
        val cardStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LightOutline.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, CARD_CORNER_RADIUS, CARD_CORNER_RADIUS, cardFillPaint)

        // Header banner
        val headerHeight = 196f
        val headerRect = RectF(
            cardRect.left,
            cardRect.top,
            cardRect.right,
            cardRect.top + headerHeight
        )
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = DeepNavy.toArgb()
            style = Paint.Style.FILL
        }
        canvas.withClip(headerRect) {
            drawRoundRect(
                RectF(cardRect.left, cardRect.top, cardRect.right, cardRect.top + headerHeight + CARD_CORNER_RADIUS),
                CARD_CORNER_RADIUS,
                CARD_CORNER_RADIUS,
                headerPaint
            )
        }

        // Brand logo inside header
        val logoSize = 104
        val logoLeft = (cardRect.left + INNER_PADDING).toInt()
        val logoTop = (cardRect.top + (headerHeight - logoSize) / 2f).toInt()
        val rawLogo = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
        if (rawLogo != null) {
            val logoBgRect = RectF(
                logoLeft.toFloat(),
                logoTop.toFloat(),
                (logoLeft + logoSize).toFloat(),
                (logoTop + logoSize).toFloat()
            )
            val logoBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = TicketPaperBackground.toArgb()
            }
            canvas.drawRoundRect(logoBgRect, 20f, 20f, logoBgPaint)
            val dstRect = Rect(
                logoLeft + 8,
                logoTop + 8,
                logoLeft + logoSize - 8,
                logoTop + logoSize - 8
            )
            canvas.drawBitmap(rawLogo, null, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
        }

        val brandTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TicketPaperBackground.toArgb()
            textSize = 44f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val brandSubtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TransitTealDark.toArgb()
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.06f
        }
        val textStartX = logoLeft + logoSize + 28f
        canvas.drawText("AeTranzit", textStartX, cardRect.top + 88f, brandTitlePaint)
        canvas.drawText("DIGITAL BUS TICKET", textStartX, cardRect.top + 132f, brandSubtitlePaint)

        // Status Pill (top-right)
        val statusText = data.statusLabel.uppercase()
        val statusTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (data.isDroppedOff) BlueOnContainerLight.toArgb() else TealOnContainerLight.toArgb()
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val statusTextWidth = statusTextPaint.measureText(statusText)
        val pillWidth = statusTextWidth + 48f
        val pillHeight = 54f
        val pillRight = cardRect.right - INNER_PADDING
        val pillLeft = pillRight - pillWidth
        val pillTop = cardRect.top + (headerHeight - pillHeight) / 2f
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (data.isDroppedOff) BlueContainerLight.toArgb() else TealContainerLight.toArgb()
        }
        canvas.drawRoundRect(
            RectF(pillLeft, pillTop, pillRight, pillTop + pillHeight),
            pillHeight / 2f,
            pillHeight / 2f,
            pillPaint
        )
        canvas.drawText(
            statusText,
            pillLeft + 24f,
            pillTop + 36f,
            statusTextPaint
        )

        var cursorY = headerRect.bottom + 44f
        val contentLeft = cardRect.left + INNER_PADDING
        val contentRight = cardRect.right - INNER_PADDING

        // Ref # & Large Seat Badge Row
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TicketPaperSubtleInk.toArgb()
            textSize = 25f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val refValuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TicketPaperInk.toArgb()
            textSize = 48f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText("TICKET REFERENCE", contentLeft, cursorY + 26f, labelPaint)
        canvas.drawText("#${data.referenceCode}", contentLeft, cursorY + 88f, refValuePaint)

        val seatBoxWidth = 220f
        val seatBoxHeight = 128f
        val seatBoxRect = RectF(
            contentRight - seatBoxWidth,
            cursorY - 8f,
            contentRight,
            cursorY - 8f + seatBoxHeight
        )
        val seatBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ElectricBlue.toArgb()
        }
        canvas.drawRoundRect(seatBoxRect, 24f, 24f, seatBoxPaint)

        val seatCaptionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TicketPaperBackground.toArgb()
            textSize = 22f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val seatNumPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TicketPaperBackground.toArgb()
            textSize = 54f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("SEAT", seatBoxRect.centerX(), seatBoxRect.top + 38f, seatCaptionPaint)
        canvas.drawText(data.seatNumber, seatBoxRect.centerX(), seatBoxRect.top + 102f, seatNumPaint)

        cursorY += 154f

        // Perforated divider with side notches
        drawPerforatedDivider(canvas, cardRect, cursorY, outerBgPaint, cardStrokePaint)
        cursorY += 52f

        // Trip & Passenger Details
        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TicketPaperInk.toArgb()
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        cursorY = drawDetailRow(canvas, "Route", data.routeLabel, contentLeft, contentRight, cursorY, labelPaint, valuePaint)
        cursorY = drawDetailRow(canvas, "Drop-off Stop", data.destination, contentLeft, contentRight, cursorY, labelPaint, valuePaint)
        cursorY = drawDetailRow(
            canvas,
            "Bus / Plate",
            "${data.busNumber} • ${data.plateNumber}",
            contentLeft,
            contentRight,
            cursorY,
            labelPaint,
            valuePaint
        )
        val passengerValue = if (data.isDiscounted) {
            "${data.passengerTypeLabel} (-20%)"
        } else {
            data.passengerTypeLabel
        }
        cursorY = drawDetailRow(canvas, "Passenger Type", passengerValue, contentLeft, contentRight, cursorY, labelPaint, valuePaint)
        cursorY = drawDetailRow(canvas, "Conductor", data.conductorName, contentLeft, contentRight, cursorY, labelPaint, valuePaint)
        cursorY = drawDetailRow(canvas, "Issued At", data.issuedTimestamp, contentLeft, contentRight, cursorY, labelPaint, valuePaint)

        cursorY += 16f

        // Fare Summary Section Box
        val fareBoxHeight = 224f
        val fareBoxRect = RectF(contentLeft, cursorY, contentRight, cursorY + fareBoxHeight)
        val fareBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TicketPaperSectionBg.toArgb()
        }
        canvas.drawRoundRect(fareBoxRect, 24f, 24f, fareBoxPaint)

        val fareHeroLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TicketPaperInk.toArgb()
            textSize = 32f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val fareHeroValuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ElectricBlue.toArgb()
            textSize = 42f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subAmountValuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TicketPaperInk.toArgb()
            textSize = 28f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val boxInnerLeft = fareBoxRect.left + 28f
        val boxInnerRight = fareBoxRect.right - 28f
        canvas.drawText("Fare Paid", boxInnerLeft, fareBoxRect.top + 62f, fareHeroLabelPaint)
        canvas.drawText(data.fareFormatted, boxInnerRight, fareBoxRect.top + 64f, fareHeroValuePaint)

        val thinLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TicketPaperDivider.toArgb()
            strokeWidth = 2f
        }
        canvas.drawLine(boxInnerLeft, fareBoxRect.top + 92f, boxInnerRight, fareBoxRect.top + 92f, thinLinePaint)

        canvas.drawText("Cash Tendered", boxInnerLeft, fareBoxRect.top + 142f, labelPaint)
        canvas.drawText(data.cashFormatted, boxInnerRight, fareBoxRect.top + 142f, subAmountValuePaint)

        canvas.drawText("Change Given", boxInnerLeft, fareBoxRect.top + 194f, labelPaint)
        canvas.drawText(data.changeFormatted, boxInnerRight, fareBoxRect.top + 194f, subAmountValuePaint)

        cursorY = fareBoxRect.bottom + 42f

        // Centered Pure-White QR Code Frame
        val qrFrameSize = QR_BITMAP_PX + 32f
        val qrLeft = (CANVAS_WIDTH - qrFrameSize) / 2f
        val qrRect = RectF(qrLeft, cursorY, qrLeft + qrFrameSize, cursorY + qrFrameSize)
        canvas.drawRoundRect(qrRect, 20f, 20f, cardFillPaint)
        canvas.drawRoundRect(qrRect, 20f, 20f, thinLinePaint)

        val qrBitmapLeft = qrRect.left + 16f
        val qrBitmapTop = qrRect.top + 16f
        canvas.drawBitmap(qrBitmap, qrBitmapLeft, qrBitmapTop, null)

        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TicketPaperSubtleInk.toArgb()
            textSize = 24f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText(
            "Scan QR to verify • Keep ticket until destination",
            CANVAS_WIDTH / 2f,
            qrRect.bottom + 46f,
            footerPaint
        )

        // Outer card border
        canvas.drawRoundRect(cardRect, CARD_CORNER_RADIUS, CARD_CORNER_RADIUS, cardStrokePaint)

        bitmap
    }

    private fun drawPerforatedDivider(
        canvas: Canvas,
        cardRect: RectF,
        y: Float,
        outerBgPaint: Paint,
        borderPaint: Paint
    ) {
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TicketPaperDivider.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 3f
            pathEffect = DashPathEffect(floatArrayOf(16f, 12f), 0f)
        }
        canvas.drawLine(cardRect.left + 32f, y, cardRect.right - 32f, y, dashPaint)

        val notchRadius = 22f
        canvas.drawCircle(cardRect.left, y, notchRadius, outerBgPaint)
        canvas.drawCircle(cardRect.right, y, notchRadius, outerBgPaint)
        canvas.drawArc(
            RectF(cardRect.left - notchRadius, y - notchRadius, cardRect.left + notchRadius, y + notchRadius),
            -90f,
            180f,
            false,
            borderPaint
        )
        canvas.drawArc(
            RectF(cardRect.right - notchRadius, y - notchRadius, cardRect.right + notchRadius, y + notchRadius),
            90f,
            180f,
            false,
            borderPaint
        )
    }

    private fun drawDetailRow(
        canvas: Canvas,
        label: String,
        value: String,
        left: Float,
        right: Float,
        topY: Float,
        labelPaint: TextPaint,
        valuePaint: TextPaint
    ): Float {
        val baseline = topY + 30f
        canvas.drawText(label, left, baseline, labelPaint)

        val maxValueWidth = (right - left) * 0.62f
        val ellipsized = TextUtils.ellipsize(
            value,
            valuePaint,
            maxValueWidth,
            TextUtils.TruncateAt.END
        ).toString()

        val alignedValuePaint = TextPaint(valuePaint).apply {
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(ellipsized, right, baseline, alignedValuePaint)
        return topY + 64f
    }

    companion object {
        private const val CANVAS_WIDTH = 1080
        private const val CANVAS_HEIGHT = 1660
        private const val CARD_MARGIN = 40f
        private const val CARD_CORNER_RADIUS = 36f
        private const val INNER_PADDING = 48f
        private const val QR_BITMAP_PX = 380
    }
}
