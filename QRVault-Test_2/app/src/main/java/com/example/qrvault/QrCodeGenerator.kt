package com.example.qrvault

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream

/** Turns text into a QR code bitmap with ZXing, and wraps it in a shareable card. */
object QrCodeGenerator {

    /** Encodes [text] as a square, black-on-white QR code of [size] × [size] pixels. */
    fun encode(text: String, size: Int = 768): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",          // names in any script survive the round trip
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2                        // quiet zone, in modules
        )
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(matrix.width * matrix.height)
        for (y in 0 until matrix.height) {
            val row = y * matrix.width
            for (x in 0 until matrix.width) {
                pixels[row + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
    }

    /**
     * The image that is saved to the gallery or shared: the QR code with the contact's
     * name and a caption underneath, so the picture makes sense on its own.
     */
    fun card(qr: Bitmap, name: String, caption: String): Bitmap {
        val padding = qr.width / 12f
        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x0F, 0x1B, 0x2A)
            textSize = qr.width / 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val captionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x0F, 0x76, 0x6E)
            textSize = qr.width / 26f
            textAlign = Paint.Align.CENTER
        }
        val width = qr.width + (padding * 2).toInt()
        val textTop = padding + qr.height + padding * 0.4f
        val height = (textTop + namePaint.textSize * 1.3f + captionPaint.textSize * 1.6f + padding).toInt()

        val card = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(card).apply {
            drawColor(Color.WHITE)
            drawBitmap(qr, padding, padding, null)
            val shownName = TextUtils.ellipsize(name, namePaint, qr.width.toFloat(), TextUtils.TruncateAt.END)
            val nameBaseline = textTop + namePaint.textSize
            drawText(shownName, 0, shownName.length, width / 2f, nameBaseline, namePaint)
            drawText(caption, width / 2f, nameBaseline + captionPaint.textSize * 1.6f, captionPaint)
        }
        return card
    }

    fun toPng(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.toByteArray()
    }
}
