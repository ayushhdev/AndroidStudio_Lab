package com.example.imageslice

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

/**
 * Screen 1: choose an image, divide it with an adjustable grid, select a box.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var gridImage: GridImageView
    private lateinit var emptyHint: TextView
    private lateinit var selectionLabel: TextView
    private lateinit var rowsLabel: TextView
    private lateinit var colsLabel: TextView
    private lateinit var rowsSeek: SeekBar
    private lateinit var colsSeek: SeekBar

    private var imageUri: Uri? = null
    private var bitmap: Bitmap? = null

    // 1. Image selection: the system Photo Picker (no storage permission required).
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                gridImage.clearSelection()
                loadImage(uri)
                updateSelectionLabel()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        applySystemBarPadding(findViewById(R.id.root))

        gridImage = findViewById(R.id.gridImage)
        emptyHint = findViewById(R.id.emptyHint)
        selectionLabel = findViewById(R.id.selectionLabel)
        rowsLabel = findViewById(R.id.rowsLabel)
        colsLabel = findViewById(R.id.colsLabel)
        rowsSeek = findViewById(R.id.rowsSeek)
        colsSeek = findViewById(R.id.colsSeek)

        // 2. Grid display: SeekBars set the number of horizontal / vertical divisions.
        val gridListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                applyGrid()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        }
        rowsSeek.setOnSeekBarChangeListener(gridListener)
        colsSeek.setOnSeekBarChangeListener(gridListener)

        // 3. Box selection: tap on the image.
        gridImage.onCellSelected = { _, _ -> updateSelectionLabel() }

        findViewById<MaterialButton>(R.id.pickButton).setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        findViewById<MaterialButton>(R.id.viewButton).setOnClickListener { openSelectedBox() }

        // Restore state after rotation.
        savedInstanceState?.let { state ->
            rowsSeek.progress = state.getInt(KEY_ROWS, 3)
            colsSeek.progress = state.getInt(KEY_COLS, 3)
            state.getString(KEY_URI)?.let { loadImage(Uri.parse(it)) }
        }
        applyGrid()
        savedInstanceState?.let { state ->
            val r = state.getInt(KEY_SEL_ROW, -1)
            val c = state.getInt(KEY_SEL_COL, -1)
            if (r >= 0 && c >= 0 && bitmap != null) gridImage.select(r, c)
        }
        updateSelectionLabel()
    }

    private fun loadImage(uri: Uri) {
        try {
            bitmap = ImageSlicer.decode(this, uri)
            imageUri = uri
            gridImage.setImageBitmap(bitmap)
            emptyHint.visibility = View.GONE
        } catch (e: Exception) {
            Toast.makeText(this, R.string.toast_load_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyGrid() {
        val rows = rowsSeek.progress
        val cols = colsSeek.progress
        rowsLabel.text = getString(R.string.rows_label, rows)
        colsLabel.text = getString(R.string.cols_label, cols)
        gridImage.setGrid(rows, cols)
        updateSelectionLabel()
    }

    private fun updateSelectionLabel() {
        selectionLabel.text = if (bitmap != null && gridImage.hasSelection) {
            getString(
                R.string.selection_label,
                gridImage.selectedRow + 1, gridImage.selectedCol + 1,
                gridImage.rows, gridImage.cols,
            )
        } else {
            getString(R.string.selection_none)
        }
    }

    // 4. Separate view: explicit Intent to SliceActivity carrying the image URI and grid info.
    private fun openSelectedBox() {
        val uri = imageUri
        if (uri == null) {
            Toast.makeText(this, R.string.toast_pick_first, Toast.LENGTH_SHORT).show()
            return
        }
        if (!gridImage.hasSelection) {
            Toast.makeText(this, R.string.toast_select_first, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, SliceActivity::class.java).apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(SliceActivity.EXTRA_ROWS, gridImage.rows)
            putExtra(SliceActivity.EXTRA_COLS, gridImage.cols)
            putExtra(SliceActivity.EXTRA_ROW, gridImage.selectedRow)
            putExtra(SliceActivity.EXTRA_COL, gridImage.selectedCol)
        }
        startActivity(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_ROWS, rowsSeek.progress)
        outState.putInt(KEY_COLS, colsSeek.progress)
        outState.putInt(KEY_SEL_ROW, gridImage.selectedRow)
        outState.putInt(KEY_SEL_COL, gridImage.selectedCol)
        imageUri?.let { outState.putString(KEY_URI, it.toString()) }
    }

    companion object {
        private const val KEY_URI = "uri"
        private const val KEY_ROWS = "rows"
        private const val KEY_COLS = "cols"
        private const val KEY_SEL_ROW = "sel_row"
        private const val KEY_SEL_COL = "sel_col"

        /** Pads [root] so content stays clear of the status and navigation bars. */
        fun applySystemBarPadding(root: View) {
            val base = root.paddingLeft
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(base + bars.left, base + bars.top, base + bars.right, base + bars.bottom)
                insets
            }
        }
    }
}
