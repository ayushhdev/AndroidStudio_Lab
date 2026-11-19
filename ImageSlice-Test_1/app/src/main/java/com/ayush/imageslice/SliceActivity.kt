package com.ayush.imageslice

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/**
 * Screen 2: shows the selected box cropped out of the image, plus every box
 * of the grid in a GridView so the user can switch to another piece.
 */
class SliceActivity : AppCompatActivity() {

    private lateinit var sliceImage: ImageView
    private lateinit var sliceInfo: TextView
    private lateinit var tileGrid: GridView

    private var rows = 1
    private var cols = 1
    private var selected = 0                 // flat index: row * cols + col
    private var tiles: List<Bitmap> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark background, so the status and navigation bar icons must be light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContentView(R.layout.activity_slice)
        MainActivity.applySystemBarPadding(findViewById(R.id.root))

        sliceImage = findViewById(R.id.sliceImage)
        sliceInfo = findViewById(R.id.sliceInfo)
        tileGrid = findViewById(R.id.tileGrid)
        findViewById<MaterialButton>(R.id.backButton).setOnClickListener { finish() }

        // Read what MainActivity sent through the Intent.
        val uri = intent.data
        rows = intent.getIntExtra(EXTRA_ROWS, 1)
        cols = intent.getIntExtra(EXTRA_COLS, 1)
        val row = intent.getIntExtra(EXTRA_ROW, 0)
        val col = intent.getIntExtra(EXTRA_COL, 0)
        selected = savedInstanceState?.getInt(KEY_SELECTED) ?: (row * cols + col)

        val source = try {
            uri?.let { ImageSlicer.decode(this, it) }
        } catch (e: Exception) {
            null
        }
        if (source == null) {
            Toast.makeText(this, R.string.toast_load_failed, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Crop every box once; the large view and the GridView both use these.
        tiles = List(rows * cols) { i -> ImageSlicer.crop(source, rows, cols, i / cols, i % cols) }

        tileGrid.numColumns = cols
        tileGrid.adapter = TileAdapter()
        tileGrid.setOnItemClickListener { _, _, position, _ -> show(position) }
        show(selected)
    }

    private fun show(index: Int) {
        selected = index
        val tile = tiles[index]
        sliceImage.setImageBitmap(tile)
        sliceInfo.text = getString(
            R.string.slice_info, index / cols + 1, index % cols + 1, tile.width, tile.height,
        )
        (tileGrid.adapter as BaseAdapter).notifyDataSetChanged()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED, selected)
    }

    /** GridView adapter: one thumbnail per box, the selected one framed in the accent colour. */
    private inner class TileAdapter : BaseAdapter() {
        private val pad = (3 * resources.displayMetrics.density).toInt()

        override fun getCount() = tiles.size
        override fun getItem(position: Int) = tiles[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = (convertView as? ImageView) ?: ImageView(this@SliceActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(pad, pad, pad, pad)
                cropToPadding = true
            }
            // Cells keep a 4:3 shape based on the column width.
            val cellWidth = tileGrid.columnWidth.takeIf { it > 0 }
                ?: ((parent.width - parent.paddingLeft - parent.paddingRight) / cols)
            val cellHeight = (cellWidth * 0.75f).toInt().coerceAtLeast(pad * 6)
            view.layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cellHeight)
            view.setImageBitmap(tiles[position])
            view.setBackgroundResource(
                if (position == selected) R.drawable.bg_tile_selected else R.drawable.bg_tile,
            )
            view.contentDescription = getString(
                R.string.slice_info, position / cols + 1, position % cols + 1,
                tiles[position].width, tiles[position].height,
            )
            return view
        }
    }

    companion object {
        const val EXTRA_ROWS = "extra_rows"
        const val EXTRA_COLS = "extra_cols"
        const val EXTRA_ROW = "extra_row"
        const val EXTRA_COL = "extra_col"
        private const val KEY_SELECTED = "selected"
    }
}
