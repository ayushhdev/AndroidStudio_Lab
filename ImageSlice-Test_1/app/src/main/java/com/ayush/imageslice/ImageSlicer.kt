package com.ayush.imageslice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri

/**
 * Image-processing helpers shared by both activities.
 *
 * The grid is defined by (rows, cols); box (row, col) is cropped using integer
 * boundaries so that all boxes together cover every pixel exactly once.
 */
object ImageSlicer {

    /** Longest edge kept in memory: large camera photos are downsampled to this. */
    private const val MAX_EDGE = 2048

    /** Decodes [uri] into a software bitmap, downsampled if it is very large. */
    fun decode(context: Context, uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            // Software allocation so Bitmap.createBitmap() can read the pixels.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val w = info.size.width
            val h = info.size.height
            val longest = maxOf(w, h)
            if (longest > MAX_EDGE) {
                val scale = MAX_EDGE.toFloat() / longest
                decoder.setTargetSize((w * scale).toInt(), (h * scale).toInt())
            }
        }
    }

    /** Pixel bounds of box (row, col) inside a [width] x [height] image. */
    fun cellBounds(width: Int, height: Int, rows: Int, cols: Int, row: Int, col: Int): Rect {
        val left = col * width / cols
        val right = (col + 1) * width / cols
        val top = row * height / rows
        val bottom = (row + 1) * height / rows
        return Rect(left, top, right, bottom)
    }

    /** Crops box (row, col) out of [bitmap]. */
    fun crop(bitmap: Bitmap, rows: Int, cols: Int, row: Int, col: Int): Bitmap {
        val r = cellBounds(bitmap.width, bitmap.height, rows, cols, row, col)
        return Bitmap.createBitmap(bitmap, r.left, r.top, r.width(), r.height())
    }
}
