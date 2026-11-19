package com.example.qrvault

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.abs

/** Pads this view so its content clears the status bar, navigation bar and display cutout (edge-to-edge). */
fun View.applySystemBarInsets() {
    val left = paddingLeft
    val top = paddingTop
    val right = paddingRight
    val bottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
        insets
    }
}

private val AVATAR_COLORS = intArrayOf(
    Color.rgb(0x0F, 0x76, 0x6E), Color.rgb(0x1D, 0x4E, 0xD8), Color.rgb(0x7C, 0x3A, 0xED),
    Color.rgb(0xB4, 0x53, 0x09), Color.rgb(0xBE, 0x18, 0x5D), Color.rgb(0x33, 0x41, 0x55)
)

/** Shows the person's initials on a circle whose colour is stable for that name. */
fun TextView.bindAvatar(name: String) {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    text = when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(1).uppercase()
        else -> (words.first().take(1) + words.last().take(1)).uppercase()
    }
    backgroundTintList = ColorStateList.valueOf(AVATAR_COLORS[abs(name.hashCode() % AVATAR_COLORS.size)])
}

fun Context.formatTimestamp(millis: Long): String = DateUtils.formatDateTime(
    this, millis,
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
)
