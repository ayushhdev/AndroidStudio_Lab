package com.example.qrvault

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

/** Writes QR images to the device gallery or to a shareable file. Call off the main thread. */
object QrImageExport {

    /** Adds [bitmap] to Pictures/QRVault through MediaStore. No storage permission is needed on API 29+. */
    fun saveToGallery(context: Context, bitmap: Bitmap, contactName: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${fileStem(contactName)}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/QRVault")
            // Hidden from other apps until the bytes are fully written.
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: throw IOException("MediaStore insert failed")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) throw IOException("PNG encode failed")
            } ?: throw IOException("Cannot open $uri")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    /**
     * Writes [bitmap] into the app cache and returns an ACTION_SEND chooser that hands other
     * apps a temporary, read-only content:// URI from our FileProvider.
     */
    fun shareIntent(context: Context, bitmap: Bitmap, contactName: String, text: String): Intent {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }          // keep only the latest shared image
        val file = File(dir, "${fileStem(contactName)}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject, contactName))
            putExtra(Intent.EXTRA_TEXT, text)
            clipData = android.content.ClipData.newRawUri(contactName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, context.getString(R.string.share_chooser))
    }

    private fun fileStem(name: String): String {
        val safe = name.replace(Regex("[^\\p{L}\\p{N}]+"), "_").trim('_').take(40).ifEmpty { "contact" }
        return "QRVault_${safe}_${System.currentTimeMillis()}"
    }
}
