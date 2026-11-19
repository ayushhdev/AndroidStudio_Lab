package com.example.qrvault

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Screen 2 — generate (or reopen) a contact's QR code, show it, and save it to the
 * gallery or share it through an Intent.
 *
 * Opened from the contact list it generates a fresh code and stores it in SQLite history;
 * opened from History it loads the stored code instead.
 */
class QrActivity : AppCompatActivity() {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var db: QrHistoryDbHelper

    private lateinit var qrImage: ImageView
    private lateinit var progress: CircularProgressIndicator
    private lateinit var contactName: TextView
    private lateinit var historyBadge: TextView
    private lateinit var phonesLabel: TextView
    private lateinit var phonesText: TextView
    private lateinit var emailsLabel: TextView
    private lateinit var emailsText: TextView
    private lateinit var noDetailsText: TextView
    private lateinit var payloadToggle: MaterialButton
    private lateinit var payloadText: TextView
    private lateinit var saveButton: MaterialButton
    private lateinit var shareButton: MaterialButton

    private var entry: HistoryEntry? = null
    private var qrBitmap: Bitmap? = null
    /** Set once the code is in the database, so a rotation reloads it instead of generating again. */
    private var historyId = NO_ID
    private var justGenerated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_qr)
        findViewById<View>(R.id.root).applySystemBarInsets()
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        db = QrHistoryDbHelper(this)
        qrImage = findViewById(R.id.qrImage)
        progress = findViewById(R.id.progress)
        contactName = findViewById(R.id.contactName)
        historyBadge = findViewById(R.id.historyBadge)
        phonesLabel = findViewById(R.id.phonesLabel)
        phonesText = findViewById(R.id.phonesText)
        emailsLabel = findViewById(R.id.emailsLabel)
        emailsText = findViewById(R.id.emailsText)
        noDetailsText = findViewById(R.id.noDetailsText)
        payloadToggle = findViewById(R.id.payloadToggle)
        payloadText = findViewById(R.id.payloadText)
        saveButton = findViewById(R.id.saveButton)
        shareButton = findViewById(R.id.shareButton)

        payloadToggle.setOnClickListener { setPayloadVisible(payloadText.visibility != View.VISIBLE) }
        saveButton.setOnClickListener { saveToGallery() }
        shareButton.setOnClickListener { share() }

        // Show the contact's name straight away while the code is being prepared.
        contactName.text = intent.getStringExtra(EXTRA_NAME).orEmpty()

        historyId = savedInstanceState?.getLong(KEY_HISTORY_ID, NO_ID) ?: intent.getLongExtra(EXTRA_HISTORY_ID, NO_ID)
        justGenerated = savedInstanceState?.getBoolean(KEY_JUST_GENERATED) ?: false
        if (historyId != NO_ID) loadFromHistory(historyId) else generate()
        if (savedInstanceState?.getBoolean(KEY_PAYLOAD_VISIBLE) == true) setPayloadVisible(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_HISTORY_ID, historyId)
        outState.putBoolean(KEY_JUST_GENERATED, justGenerated)
        outState.putBoolean(KEY_PAYLOAD_VISIBLE, payloadText.visibility == View.VISIBLE)
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
        db.close()
    }

    // ---- 3. QR code generation + 5. store in SQLite history ----

    private fun generate() {
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        val phones = intent.getStringArrayListExtra(EXTRA_PHONES).orEmpty()
        val emails = intent.getStringArrayListExtra(EXTRA_EMAILS).orEmpty()

        executor.execute {
            val result = runCatching {
                val payload = VCard.build(name, phones, emails)
                val qr = QrCodeGenerator.encode(payload)
                val id = db.save(name, phones, emails, payload, QrCodeGenerator.toPng(qr))
                checkNotNull(db.get(id)) to qr
            }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                result.onSuccess { (saved, qr) ->
                    historyId = saved.id
                    justGenerated = true
                    show(saved, qr)
                }.onFailure {
                    progress.visibility = View.GONE
                    Toast.makeText(this, R.string.qr_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadFromHistory(id: Long) {
        executor.execute {
            val saved = db.get(id)
            val qr = saved?.let { BitmapFactory.decodeByteArray(it.qrPng, 0, it.qrPng.size) }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                if (saved == null || qr == null) {
                    Toast.makeText(this, R.string.history_missing, Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    show(saved, qr)
                }
            }
        }
    }

    // ---- 4. Display ----

    private fun show(saved: HistoryEntry, qr: Bitmap) {
        entry = saved
        qrBitmap = qr
        progress.visibility = View.GONE
        qrImage.setImageBitmap(qr)
        qrImage.contentDescription = getString(R.string.cd_qr, saved.contactName)
        contactName.text = saved.contactName

        val time = formatTimestamp(saved.createdAt)
        historyBadge.text = getString(if (justGenerated) R.string.qr_saved_history else R.string.qr_from_history, time)
        historyBadge.visibility = View.VISIBLE

        phonesText.text = saved.phones.joinToString("\n")
        emailsText.text = saved.emails.joinToString("\n")
        val hasPhones = saved.phones.isNotEmpty()
        val hasEmails = saved.emails.isNotEmpty()
        phonesLabel.visibility = if (hasPhones) View.VISIBLE else View.GONE
        phonesText.visibility = phonesLabel.visibility
        emailsLabel.visibility = if (hasEmails) View.VISIBLE else View.GONE
        emailsText.visibility = emailsLabel.visibility
        noDetailsText.visibility = if (hasPhones || hasEmails) View.GONE else View.VISIBLE
        findViewById<View>(R.id.detailsCard).visibility = View.VISIBLE

        payloadText.text = saved.payload
        saveButton.isEnabled = true
        shareButton.isEnabled = true
    }

    private fun setPayloadVisible(visible: Boolean) {
        payloadText.visibility = if (visible) View.VISIBLE else View.GONE
        payloadToggle.setText(if (visible) R.string.hide_payload else R.string.show_payload)
    }

    // ---- 4. Save to gallery / share ----

    private fun saveToGallery() {
        val saved = entry ?: return
        val qr = qrBitmap ?: return
        saveButton.isEnabled = false
        executor.execute {
            val result = runCatching {
                val card = QrCodeGenerator.card(qr, saved.contactName, getString(R.string.card_caption))
                QrImageExport.saveToGallery(this, card, saved.contactName)
            }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                saveButton.isEnabled = true
                Toast.makeText(this, if (result.isSuccess) R.string.saved_gallery else R.string.save_failed,
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun share() {
        val saved = entry ?: return
        val qr = qrBitmap ?: return
        shareButton.isEnabled = false
        executor.execute {
            val result = runCatching {
                val card = QrCodeGenerator.card(qr, saved.contactName, getString(R.string.card_caption))
                val text = buildString {
                    append(saved.contactName)
                    saved.phones.forEach { append("\n").append(it) }
                    saved.emails.forEach { append("\n").append(it) }
                }
                QrImageExport.shareIntent(this, card, saved.contactName, text)
            }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                shareButton.isEnabled = true
                result.onSuccess { startActivity(it) }
                    .onFailure { Toast.makeText(this, R.string.share_failed, Toast.LENGTH_SHORT).show() }
            }
        }
    }

    companion object {
        private const val EXTRA_NAME = "name"
        private const val EXTRA_PHONES = "phones"
        private const val EXTRA_EMAILS = "emails"
        private const val EXTRA_HISTORY_ID = "history_id"
        private const val KEY_HISTORY_ID = "history_id"
        private const val KEY_JUST_GENERATED = "just_generated"
        private const val KEY_PAYLOAD_VISIBLE = "payload_visible"
        private const val NO_ID = -1L

        /** Explicit Intent: generate a new QR code for [contact]. */
        fun forContact(context: Context, contact: ContactInfo) = Intent(context, QrActivity::class.java)
            .putExtra(EXTRA_NAME, contact.name)
            .putStringArrayListExtra(EXTRA_PHONES, ArrayList(contact.phones))
            .putStringArrayListExtra(EXTRA_EMAILS, ArrayList(contact.emails))

        /** Explicit Intent: reopen a QR code saved in history. */
        fun forHistory(context: Context, entry: HistoryEntry) = Intent(context, QrActivity::class.java)
            .putExtra(EXTRA_NAME, entry.contactName)
            .putExtra(EXTRA_HISTORY_ID, entry.id)
    }
}
