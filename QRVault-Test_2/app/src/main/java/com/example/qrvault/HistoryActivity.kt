package com.example.qrvault

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Screen 3 — every QR code generated so far, read from SQLite, newest first. */
class HistoryActivity : AppCompatActivity() {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var db: QrHistoryDbHelper
    private lateinit var adapter: HistoryAdapter

    private lateinit var toolbar: MaterialToolbar
    private lateinit var list: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var progress: CircularProgressIndicator
    private var loadedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)
        findViewById<View>(R.id.root).applySystemBarInsets()

        db = QrHistoryDbHelper(this)
        toolbar = findViewById(R.id.toolbar)
        list = findViewById(R.id.historyList)
        emptyText = findViewById(R.id.emptyText)
        progress = findViewById(R.id.progress)

        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_clear) { confirmClear(); true } else false
        }

        adapter = HistoryAdapter(
            onOpen = { entry -> startActivity(QrActivity.forHistory(this, entry)) },
            onDelete = ::delete
        )
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        list.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
            ContextCompat.getDrawable(this@HistoryActivity, R.drawable.divider)?.let(::setDrawable)
        })
    }

    override fun onStart() {
        super.onStart()
        load()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
        db.close()
    }

    private fun load() {
        if (!loadedOnce) progress.visibility = View.VISIBLE
        executor.execute {
            // Decode small thumbnails here rather than in onBindViewHolder on the main thread.
            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
            val rows = db.getAll().map { entry ->
                HistoryRow(entry, BitmapFactory.decodeByteArray(entry.qrPng, 0, entry.qrPng.size, options))
            }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                loadedOnce = true
                progress.visibility = View.GONE
                adapter.submitList(rows)
                toolbar.subtitle = resources.getQuantityString(R.plurals.history_count, rows.size, rows.size)
                emptyText.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
                toolbar.menu.findItem(R.id.action_clear)?.isEnabled = rows.isNotEmpty()
            }
        }
    }

    private fun delete(entry: HistoryEntry) {
        executor.execute {
            db.delete(entry.id)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                load()
                Snackbar.make(list, getString(R.string.deleted_entry, entry.contactName), Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) { executor.execute { db.restore(entry); runOnUiThread { load() } } }
                    .show()
            }
        }
    }

    private fun confirmClear() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_confirm_title)
            .setMessage(R.string.clear_confirm_body)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.clear_all) { _, _ ->
                executor.execute { db.clear(); runOnUiThread { if (!isDestroyed) load() } }
            }
            .show()
    }
}
