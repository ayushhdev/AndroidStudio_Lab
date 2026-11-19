package com.example.qrvault

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Screen 1 — fetch contacts through the Contacts Content Provider, list them in a
 * RecyclerView and let the user pick one to turn into a QR code.
 */
class MainActivity : AppCompatActivity() {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var repository: ContactsRepository
    private lateinit var adapter: ContactAdapter

    private lateinit var toolbar: MaterialToolbar
    private lateinit var searchLayout: TextInputLayout
    private lateinit var searchInput: TextInputEditText
    private lateinit var list: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var progress: CircularProgressIndicator
    private lateinit var permissionPanel: LinearLayout
    private lateinit var grantButton: MaterialButton

    private var allContacts: List<ContactInfo> = emptyList()
    private var loadedOnce = false
    /** True once the user picked "Don't allow" twice; only Settings can grant it after that. */
    private var permanentlyDenied = false

    private val contactsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                loadContacts()
            } else {
                permanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)
                showPermissionPanel()
            }
        }

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchScanner()
            else Toast.makeText(this, R.string.camera_denied, Toast.LENGTH_LONG).show()
        }

    // ZXing's camera scanner, launched through the Activity Result API.
    private val scanner = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let(::showScanResult)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.root).applySystemBarInsets()

        repository = ContactsRepository(this)
        toolbar = findViewById(R.id.toolbar)
        searchLayout = findViewById(R.id.searchLayout)
        searchInput = findViewById(R.id.searchInput)
        list = findViewById(R.id.contactsList)
        emptyText = findViewById(R.id.emptyText)
        progress = findViewById(R.id.progress)
        permissionPanel = findViewById(R.id.permissionPanel)
        grantButton = findViewById(R.id.grantButton)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_scan -> { startScan(); true }
                R.id.action_history -> { startActivity(Intent(this, HistoryActivity::class.java)); true }
                R.id.action_about -> { showAbout(); true }
                else -> false
            }
        }

        adapter = ContactAdapter { contact -> startActivity(QrActivity.forContact(this, contact)) }
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        list.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
            ContextCompat.getDrawable(this@MainActivity, R.drawable.divider)?.let(::setDrawable)
        })

        searchInput.doAfterTextChanged { applyFilter() }
        grantButton.setOnClickListener { onGrantClicked() }

        if (savedInstanceState == null && !hasContactsPermission()) {
            contactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    override fun onStart() {
        super.onStart()
        // Reload every time the screen returns, so contacts added meanwhile show up.
        if (hasContactsPermission()) loadContacts() else showPermissionPanel()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }

    private fun hasContactsPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    private fun onGrantClicked() {
        if (permanentlyDenied) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        } else {
            contactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun showPermissionPanel() {
        permissionPanel.visibility = View.VISIBLE
        grantButton.setText(if (permanentlyDenied) R.string.permission_settings else R.string.permission_grant)
        searchLayout.visibility = View.GONE
        list.visibility = View.GONE
        emptyText.visibility = View.GONE
        progress.visibility = View.GONE
        toolbar.subtitle = null
    }

    // ---- 1. Fetch contacts (background thread) ----

    private fun loadContacts() {
        permissionPanel.visibility = View.GONE
        searchLayout.visibility = View.VISIBLE
        list.visibility = View.VISIBLE
        if (!loadedOnce) progress.visibility = View.VISIBLE

        executor.execute {
            val result = runCatching { repository.loadAll() }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                progress.visibility = View.GONE
                result.onSuccess { contacts ->
                    loadedOnce = true
                    allContacts = contacts
                    applyFilter()
                }.onFailure { error ->
                    Toast.makeText(this, error.message ?: error.javaClass.simpleName, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ---- 2. Contact selection list, filtered by the search box ----

    private fun applyFilter() {
        if (!loadedOnce) return
        val query = searchInput.text?.toString()?.trim().orEmpty()
        val digits = query.filter { it.isDigit() }
        val shown = if (query.isEmpty()) allContacts else allContacts.filter { c ->
            c.name.contains(query, ignoreCase = true) ||
                c.emails.any { it.contains(query, ignoreCase = true) } ||
                (digits.isNotEmpty() && c.phones.any { phone -> phone.filter { it.isDigit() }.contains(digits) })
        }
        adapter.submitList(shown)

        toolbar.subtitle = if (query.isEmpty()) resources.getQuantityString(R.plurals.contacts_count, allContacts.size, allContacts.size)
        else getString(R.string.contacts_count_filtered, shown.size, allContacts.size)

        emptyText.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        emptyText.text = if (allContacts.isEmpty()) getString(R.string.no_contacts)
        else getString(R.string.no_matches, query)
    }

    // ---- Scan a QR code with the camera ----

    private fun startScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchScanner()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchScanner() {
        scanner.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt(getString(R.string.scan_prompt))
                .setBeepEnabled(false)
                .setOrientationLocked(false)
        )
    }

    private fun showScanResult(text: String) {
        val contact = VCard.parse(text)
        val dialog = MaterialAlertDialogBuilder(this)
            .setNeutralButton(R.string.scan_copy) { _, _ -> copyToClipboard(text) }
            .setNegativeButton(R.string.close, null)
        if (contact != null) {
            val details = buildString {
                append(contact.name)
                contact.phones.forEach { append("\n").append(it) }
                contact.emails.forEach { append("\n").append(it) }
            }
            dialog.setTitle(R.string.scan_contact_title)
                .setMessage(details)
                .setPositiveButton(R.string.scan_add_contact) { _, _ -> addToContacts(contact) }
        } else {
            dialog.setTitle(R.string.scan_text_title).setMessage(text)
        }
        dialog.show()
    }

    /** Opens the system Contacts app pre-filled with the scanned details (no WRITE_CONTACTS needed). */
    private fun addToContacts(contact: VCard.Parsed) {
        val rows = ArrayList<ContentValues>()
        contact.phones.forEach { phone ->
            rows += ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                put(Phone.NUMBER, phone)
                put(Phone.TYPE, Phone.TYPE_MOBILE)
            }
        }
        contact.emails.forEach { email ->
            rows += ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                put(Email.ADDRESS, email)
                put(Email.TYPE, Email.TYPE_OTHER)
            }
        }
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, contact.name)
            putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, rows)
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_contacts_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAbout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.app_name)
            .setMessage(R.string.about_body)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun copyToClipboard(text: String) {
        getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), text))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }
}
