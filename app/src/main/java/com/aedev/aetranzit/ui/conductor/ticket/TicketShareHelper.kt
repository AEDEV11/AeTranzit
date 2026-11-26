package com.aedev.aetranzit.ui.conductor.ticket

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.aedev.aetranzit.di.IoDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TicketShareHelper @Inject constructor(
    private val bitmapRenderer: DigitalTicketBitmapRenderer,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun shareTicketImage(
        context: Context,
        renderData: DigitalTicketRenderData
    ): Result<Unit> {
        return try {
            val bitmap = bitmapRenderer.renderTicketBitmap(context, renderData)
            val contentUri = writeTicketBitmapToCache(
                context = context,
                bitmap = bitmap,
                referenceCode = renderData.referenceCode
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = MIME_IMAGE_PNG
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "AeTranzit Ticket #${renderData.referenceCode}"
                )
                clipData = ClipData.newRawUri(
                    "AeTranzit Ticket #${renderData.referenceCode}",
                    contentUri
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(
                shareIntent,
                "Share Ticket #${renderData.referenceCode}"
            ).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            context.startActivity(chooserIntent)
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: ActivityNotFoundException) {
            Result.failure(
                IllegalStateException("No compatible app available to share ticket images.")
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message ?: "Unable to render or share the digital ticket image."
                )
            )
        }
    }

    fun launchSmsReceiptIntent(
        context: Context,
        smsBody: String
    ): Result<Unit> {
        return try {
            val cleanBody = smsBody.trim()
            if (cleanBody.isEmpty()) {
                return Result.failure(
                    IllegalArgumentException("SMS receipt body is empty.")
                )
            }

            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = "smsto:".toUri()
                putExtra("sms_body", cleanBody)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            context.startActivity(smsIntent)
            Result.success(Unit)
        } catch (_: ActivityNotFoundException) {
            Result.failure(
                IllegalStateException("No SMS messaging app found on this device.")
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message ?: "Unable to open the SMS app."
                )
            )
        }
    }

    private suspend fun writeTicketBitmapToCache(
        context: Context,
        bitmap: Bitmap,
        referenceCode: String
    ): Uri = withContext(ioDispatcher) {
        val ticketsDir = File(context.cacheDir, TICKETS_CACHE_DIR)
        if (!ticketsDir.exists() && !ticketsDir.mkdirs()) {
            throw IllegalStateException("Unable to prepare local cache directory for ticket sharing.")
        }

        pruneOldCachedTickets(ticketsDir)

        val safeRef = referenceCode.replace(SAFE_FILENAME_REGEX, "_").ifBlank { "ticket" }
        val ticketFile = File(ticketsDir, "ticket_$safeRef.png")
        FileOutputStream(ticketFile).use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw IllegalStateException("Failed to encode ticket PNG image.")
            }
            output.flush()
        }

        FileProvider.getUriForFile(
            context,
            "${context.packageName}$FILE_PROVIDER_SUFFIX",
            ticketFile
        )
    }

    private fun pruneOldCachedTickets(ticketsDir: File) {
        val existingFiles = ticketsDir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        if (existingFiles.size > MAX_CACHED_TICKETS) {
            existingFiles.drop(MAX_CACHED_TICKETS).forEach { it.delete() }
        }
    }

    companion object {
        private const val TICKETS_CACHE_DIR = "tickets"
        private const val FILE_PROVIDER_SUFFIX = ".fileprovider"
        private const val MIME_IMAGE_PNG = "image/png"
        private const val MAX_CACHED_TICKETS = 20
        private val SAFE_FILENAME_REGEX = Regex("[^A-Za-z0-9_-]")
    }
}
