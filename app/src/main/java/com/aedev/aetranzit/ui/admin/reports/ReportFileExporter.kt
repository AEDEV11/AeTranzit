package com.aedev.aetranzit.ui.admin.reports

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.aedev.aetranzit.di.IoDispatcher
import com.aedev.aetranzit.domain.usecase.ReportExportFormatter
import com.aedev.aetranzit.domain.usecase.ReportTicketRow
import com.aedev.aetranzit.ui.components.formatPhp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportFileExporter @Inject constructor(
    private val formatter: ReportExportFormatter,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun exportCsvAndShare(
        context: Context,
        rows: List<ReportTicketRow>,
        filterSummary: String
    ): Result<Unit> {
        return try {
            val contentUri = withContext(ioDispatcher) {
                val csvContent = formatter.buildCsvContent(rows)
                val reportsDir = ensureReportsCacheDir(context)
                pruneOldReports(reportsDir)

                val fileName = "aetranzit_tickets_${fileTimestamp()}.csv"
                val csvFile = File(reportsDir, fileName)
                csvFile.writeText(csvContent, Charsets.UTF_8)

                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}$FILE_PROVIDER_SUFFIX",
                    csvFile
                )
            }

            launchShareSheet(
                context = context,
                contentUri = contentUri,
                mimeType = MIME_CSV,
                subject = "AeTranzit Ticket Report ($filterSummary)",
                chooserTitle = "Export Ticket CSV Report"
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: ActivityNotFoundException) {
            Result.failure(
                IllegalStateException("No compatible app found to share CSV reports.")
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(e.message ?: "Failed to generate CSV report.")
            )
        }
    }

    suspend fun exportPdfAndShare(
        context: Context,
        rows: List<ReportTicketRow>,
        filterSummary: String,
        totalRevenue: Double
    ): Result<Unit> {
        return try {
            val contentUri = withContext(ioDispatcher) {
                val reportsDir = ensureReportsCacheDir(context)
                pruneOldReports(reportsDir)

                val fileName = "aetranzit_report_${fileTimestamp()}.pdf"
                val pdfFile = File(reportsDir, fileName)
                writePdfReport(
                    outputFile = pdfFile,
                    rows = rows,
                    filterSummary = filterSummary,
                    totalRevenue = totalRevenue
                )

                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}$FILE_PROVIDER_SUFFIX",
                    pdfFile
                )
            }

            launchShareSheet(
                context = context,
                contentUri = contentUri,
                mimeType = MIME_PDF,
                subject = "AeTranzit Operations Report ($filterSummary)",
                chooserTitle = "Export Ticket PDF Report"
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: ActivityNotFoundException) {
            Result.failure(
                IllegalStateException("No compatible app found to share PDF reports.")
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(e.message ?: "Failed to generate PDF report.")
            )
        }
    }

    private fun launchShareSheet(
        context: Context,
        contentUri: Uri,
        mimeType: String,
        subject: String,
        chooserTitle: String
    ): Result<Unit> {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            clipData = ClipData.newRawUri(subject, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(chooser)
        return Result.success(Unit)
    }

    private fun writePdfReport(
        outputFile: File,
        rows: List<ReportTicketRow>,
        filterSummary: String,
        totalRevenue: Double
    ) {
        val document = PdfDocument()
        try {
            val chunks = if (rows.isEmpty()) listOf(emptyList()) else rows.chunked(ROWS_PER_PDF_PAGE)
            val totalPages = chunks.size
            val generatedAt = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US).format(Date())
            val discountedCount = rows.count { it.isDiscounted }

            chunks.forEachIndexed { index, pageRows ->
                val pageNumber = index + 1
                val pageInfo = PdfDocument.PageInfo.Builder(
                    PDF_PAGE_WIDTH,
                    PDF_PAGE_HEIGHT,
                    pageNumber
                ).create()
                val page = document.startPage(pageInfo)
                drawPdfPage(
                    canvas = page.canvas,
                    pageRows = pageRows,
                    filterSummary = filterSummary,
                    generatedAt = generatedAt,
                    totalTickets = rows.size,
                    discountedCount = discountedCount,
                    totalRevenue = totalRevenue,
                    pageNumber = pageNumber,
                    totalPages = totalPages
                )
                document.finishPage(page)
            }

            FileOutputStream(outputFile).use { stream ->
                document.writeTo(stream)
                stream.flush()
            }
        } finally {
            document.close()
        }
    }

    private fun drawPdfPage(
        canvas: Canvas,
        pageRows: List<ReportTicketRow>,
        filterSummary: String,
        generatedAt: String,
        totalTickets: Int,
        discountedCount: Int,
        totalRevenue: Double,
        pageNumber: Int,
        totalPages: Int
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, PDF_PAGE_WIDTH.toFloat(), PDF_PAGE_HEIGHT.toFloat(), bgPaint)

        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PDF_NAVY_HEX }
        canvas.drawRect(0f, 0f, PDF_PAGE_WIDTH.toFloat(), 76f, headerBgPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PDF_SUBTITLE_HEX
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        canvas.drawText("AeTranzit — Operations & Ticket Report", MARGIN_X, 32f, titlePaint)
        canvas.drawText(
            "Filters: $filterSummary   •   Generated: $generatedAt",
            MARGIN_X,
            54f,
            subtitlePaint
        )

        val summaryCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PDF_SUMMARY_BG_HEX }
        val summaryRect = RectF(MARGIN_X, 90f, PDF_PAGE_WIDTH - MARGIN_X, 132f)
        canvas.drawRoundRect(summaryRect, 6f, 6f, summaryCardPaint)

        val kpiLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PDF_MUTED_TEXT_HEX
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val kpiValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PDF_NAVY_HEX
            textSize = 13f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        canvas.drawText("TOTAL REVENUE", MARGIN_X + 14f, 106f, kpiLabelPaint)
        canvas.drawText(formatPhp(totalRevenue), MARGIN_X + 14f, 123f, kpiValuePaint)

        canvas.drawText("TICKETS ISSUED", MARGIN_X + 210f, 106f, kpiLabelPaint)
        canvas.drawText("$totalTickets", MARGIN_X + 210f, 123f, kpiValuePaint)

        canvas.drawText("DISCOUNTED (-20%)", MARGIN_X + 380f, 106f, kpiLabelPaint)
        canvas.drawText("$discountedCount", MARGIN_X + 380f, 123f, kpiValuePaint)

        val tableTop = 148f
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PDF_TABLE_HEADER_HEX }
        canvas.drawRect(MARGIN_X, tableTop, PDF_PAGE_WIDTH - MARGIN_X, tableTop + 22f, tableHeaderPaint)

        val colHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PDF_NAVY_HEX
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        PDF_COLUMNS.forEach { col ->
            canvas.drawText(col.title, col.x, tableTop + 15f, colHeaderTextPaint)
        }

        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PDF_NAVY_HEX
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val amountCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PDF_NAVY_HEX
            textSize = 8.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val zebraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PDF_ZEBRA_ROW_HEX }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PDF_DIVIDER_HEX
            strokeWidth = 0.6f
        }

        if (pageRows.isEmpty()) {
            canvas.drawText(
                "No ticket records matched the selected filters.",
                MARGIN_X + 12f,
                tableTop + 48f,
                cellPaint
            )
        } else {
            var currentY = tableTop + 22f
            pageRows.forEachIndexed { rowIdx, row ->
                if (rowIdx % 2 == 1) {
                    canvas.drawRect(
                        MARGIN_X,
                        currentY,
                        PDF_PAGE_WIDTH - MARGIN_X,
                        currentY + ROW_HEIGHT,
                        zebraPaint
                    )
                }

                val baselineY = currentY + 13.5f
                val values = listOf(
                    row.formattedDateTime,
                    "#${row.referenceCode}",
                    row.busNumber,
                    truncate(row.routeLabel, 22),
                    row.seatNumber,
                    truncate(row.destination, 18),
                    truncate(row.passengerTypeLabel, 14),
                    formatPhp(row.fareAmount),
                    formatPhp(row.cashTendered),
                    formatPhp(row.changeAmount),
                    row.statusLabel,
                    truncate(row.conductorName, 16)
                )

                PDF_COLUMNS.forEachIndexed { colIdx, col ->
                    val paint = if (colIdx in 7..9) amountCellPaint else cellPaint
                    canvas.drawText(values[colIdx], col.x, baselineY, paint)
                }

                currentY += ROW_HEIGHT
                canvas.drawLine(MARGIN_X, currentY, PDF_PAGE_WIDTH - MARGIN_X, currentY, dividerPaint)
            }
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PDF_MUTED_TEXT_HEX
            textSize = 8.5f
        }
        canvas.drawText(
            "AeTranzit Offline-First Fleet Analytics",
            MARGIN_X,
            PDF_PAGE_HEIGHT - 18f,
            footerPaint
        )
        canvas.drawText(
            "Page $pageNumber of $totalPages",
            PDF_PAGE_WIDTH - MARGIN_X - 68f,
            PDF_PAGE_HEIGHT - 18f,
            footerPaint
        )
    }

    private fun truncate(text: String, maxChars: Int): String {
        val clean = text.trim()
        return if (clean.length <= maxChars) clean else clean.take(maxChars - 1) + "…"
    }

    private fun ensureReportsCacheDir(context: Context): File {
        val reportsDir = File(context.cacheDir, REPORTS_CACHE_DIR)
        if (!reportsDir.exists() && !reportsDir.mkdirs()) {
            throw IllegalStateException("Unable to create local reports cache directory.")
        }
        return reportsDir
    }

    private fun pruneOldReports(reportsDir: File) {
        val existing = reportsDir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        if (existing.size > MAX_CACHED_REPORTS) {
            existing.drop(MAX_CACHED_REPORTS).forEach { it.delete() }
        }
    }

    private fun fileTimestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private data class PdfColSpec(val title: String, val x: Float)

    companion object {
        private const val REPORTS_CACHE_DIR = "reports"
        private const val FILE_PROVIDER_SUFFIX = ".fileprovider"
        private const val MIME_CSV = "text/csv"
        private const val MIME_PDF = "application/pdf"
        private const val MAX_CACHED_REPORTS = 15

        private const val PDF_PAGE_WIDTH = 842
        private const val PDF_PAGE_HEIGHT = 595
        private const val MARGIN_X = 28f
        private const val ROW_HEIGHT = 19f
        private const val ROWS_PER_PDF_PAGE = 20

        private const val PDF_NAVY_HEX = 0xFF0A192F.toInt()
        private const val PDF_SUBTITLE_HEX = 0xFFD8EBFF.toInt()
        private const val PDF_SUMMARY_BG_HEX = 0xFFF3F6FB.toInt()
        private const val PDF_TABLE_HEADER_HEX = 0xFFE6ECF5.toInt()
        private const val PDF_ZEBRA_ROW_HEX = 0xFFF9FBFE.toInt()
        private const val PDF_DIVIDER_HEX = 0xFFD5DFEE.toInt()
        private const val PDF_MUTED_TEXT_HEX = 0xFF3E4E68.toInt()

        private val PDF_COLUMNS = listOf(
            PdfColSpec("Date/Time", 32f),
            PdfColSpec("Ref #", 118f),
            PdfColSpec("Bus", 174f),
            PdfColSpec("Route", 218f),
            PdfColSpec("Seat", 334f),
            PdfColSpec("Destination", 368f),
            PdfColSpec("Type", 464f),
            PdfColSpec("Fare", 542f),
            PdfColSpec("Cash", 602f),
            PdfColSpec("Change", 662f),
            PdfColSpec("Status", 718f),
            PdfColSpec("Conductor", 764f)
        )
    }
}
