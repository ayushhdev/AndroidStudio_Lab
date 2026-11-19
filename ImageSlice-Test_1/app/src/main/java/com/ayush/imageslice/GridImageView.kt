package com.ayush.imageslice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat

/**
 * An ImageView that draws an adjustable rows x cols grid on top of the image
 * it displays and lets the user tap a box to select it.
 *
 * The grid is drawn only over the visible image area (not the empty letterbox
 * space around it), found by mapping the drawable bounds through imageMatrix.
 */
class GridImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatImageView(context, attrs) {

    var rows = 3
        private set
    var cols = 3
        private set
    var selectedRow = -1
        private set
    var selectedCol = -1
        private set

    /** Called with (row, col) when the user taps a box. */
    var onCellSelected: ((Int, Int) -> Unit)? = null

    private val density = resources.displayMetrics.density

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.grid_line)
        strokeWidth = 1.5f * density
        style = Paint.Style.STROKE
        setShadowLayer(2f * density, 0f, 0f, 0x99000000.toInt())
    }
    private val highlightFill = Paint().apply {
        color = ContextCompat.getColor(context, R.color.cell_highlight)
        style = Paint.Style.FILL
    }
    private val highlightStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.accent)
        strokeWidth = 3f * density
        style = Paint.Style.STROKE
    }

    val hasSelection: Boolean get() = selectedRow >= 0 && selectedCol >= 0

    fun setGrid(newRows: Int, newCols: Int) {
        rows = newRows.coerceAtLeast(1)
        cols = newCols.coerceAtLeast(1)
        // Drop the selection if it no longer exists in the smaller grid.
        if (selectedRow >= rows || selectedCol >= cols) clearSelection()
        invalidate()
    }

    fun select(row: Int, col: Int) {
        selectedRow = row
        selectedCol = col
        invalidate()
    }

    fun clearSelection() {
        selectedRow = -1
        selectedCol = -1
        invalidate()
    }

    /** Where the image is actually drawn, in view coordinates; null if no image. */
    private fun imageRect(): RectF? {
        val d = drawable ?: return null
        if (d.intrinsicWidth <= 0 || d.intrinsicHeight <= 0) return null
        val rect = RectF(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        imageMatrix.mapRect(rect)
        rect.offset(paddingLeft.toFloat(), paddingTop.toFloat())
        return rect
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val r = imageRect() ?: return
        val cellW = r.width() / cols
        val cellH = r.height() / rows

        if (hasSelection) {
            canvas.drawRect(
                r.left + selectedCol * cellW,
                r.top + selectedRow * cellH,
                r.left + (selectedCol + 1) * cellW,
                r.top + (selectedRow + 1) * cellH,
                highlightFill,
            )
        }

        // Vertical lines (columns) and horizontal lines (rows), including the outer frame.
        for (c in 0..cols) {
            val x = r.left + c * cellW
            canvas.drawLine(x, r.top, x, r.bottom, linePaint)
        }
        for (rr in 0..rows) {
            val y = r.top + rr * cellH
            canvas.drawLine(r.left, y, r.right, y, linePaint)
        }

        if (hasSelection) {
            val inset = highlightStroke.strokeWidth / 2
            canvas.drawRect(
                r.left + selectedCol * cellW + inset,
                r.top + selectedRow * cellH + inset,
                r.left + (selectedCol + 1) * cellW - inset,
                r.top + (selectedRow + 1) * cellH - inset,
                highlightStroke,
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val r = imageRect() ?: return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> return r.contains(event.x, event.y)
            MotionEvent.ACTION_UP -> {
                if (!r.contains(event.x, event.y)) return false
                val col = ((event.x - r.left) / (r.width() / cols)).toInt().coerceIn(0, cols - 1)
                val row = ((event.y - r.top) / (r.height() / rows)).toInt().coerceIn(0, rows - 1)
                select(row, col)
                onCellSelected?.invoke(row, col)
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()
}
