package com.example.qrvault

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** One generated QR code and the contact details it encodes. */
data class HistoryEntry(
    val id: Long,
    val contactName: String,
    val phones: List<String>,
    val emails: List<String>,
    val payload: String,
    val qrPng: ByteArray,
    val createdAt: Long
)

/** SQLite store for QR history. The QR image itself is kept as a PNG BLOB. */
class QrHistoryDbHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL,
                $COL_PHONES TEXT NOT NULL,
                $COL_EMAILS TEXT NOT NULL,
                $COL_PAYLOAD TEXT NOT NULL UNIQUE,
                $COL_QR BLOB NOT NULL,
                $COL_CREATED INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    /**
     * Saves a QR code and returns its row ID. Generating the same contact again does not
     * duplicate it: the existing row is moved to the top of the history instead.
     */
    fun save(name: String, phones: List<String>, emails: List<String>, payload: String, qrPng: ByteArray): Long {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        db.query(TABLE, arrayOf(COL_ID), "$COL_PAYLOAD = ?", arrayOf(payload), null, null, null).use { c ->
            if (c.moveToFirst()) {
                val id = c.getLong(0)
                db.update(TABLE, ContentValues().apply { put(COL_CREATED, now) },
                    "$COL_ID = ?", arrayOf(id.toString()))
                return id
            }
        }
        val values = ContentValues().apply {
            put(COL_NAME, name)
            put(COL_PHONES, phones.joinToString(SEPARATOR))
            put(COL_EMAILS, emails.joinToString(SEPARATOR))
            put(COL_PAYLOAD, payload)
            put(COL_QR, qrPng)
            put(COL_CREATED, now)
        }
        return db.insertOrThrow(TABLE, null, values)
    }

    /** Re-inserts a deleted entry with its original ID (used by Undo). */
    fun restore(entry: HistoryEntry) {
        val values = ContentValues().apply {
            put(COL_ID, entry.id)
            put(COL_NAME, entry.contactName)
            put(COL_PHONES, entry.phones.joinToString(SEPARATOR))
            put(COL_EMAILS, entry.emails.joinToString(SEPARATOR))
            put(COL_PAYLOAD, entry.payload)
            put(COL_QR, entry.qrPng)
            put(COL_CREATED, entry.createdAt)
        }
        writableDatabase.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun getAll(): List<HistoryEntry> =
        readableDatabase.query(TABLE, null, null, null, null, null, "$COL_CREATED DESC").use { c ->
            buildList { while (c.moveToNext()) add(c.toEntry()) }
        }

    fun get(id: Long): HistoryEntry? =
        readableDatabase.query(TABLE, null, "$COL_ID = ?", arrayOf(id.toString()), null, null, null).use { c ->
            if (c.moveToFirst()) c.toEntry() else null
        }

    fun delete(id: Long): Int = writableDatabase.delete(TABLE, "$COL_ID = ?", arrayOf(id.toString()))

    fun clear(): Int = writableDatabase.delete(TABLE, null, null)

    private fun Cursor.toEntry() = HistoryEntry(
        id = getLong(getColumnIndexOrThrow(COL_ID)),
        contactName = getString(getColumnIndexOrThrow(COL_NAME)),
        phones = getString(getColumnIndexOrThrow(COL_PHONES)).splitList(),
        emails = getString(getColumnIndexOrThrow(COL_EMAILS)).splitList(),
        payload = getString(getColumnIndexOrThrow(COL_PAYLOAD)),
        qrPng = getBlob(getColumnIndexOrThrow(COL_QR)),
        createdAt = getLong(getColumnIndexOrThrow(COL_CREATED))
    )

    private fun String.splitList() = if (isEmpty()) emptyList() else split(SEPARATOR)

    companion object {
        private const val DB_NAME = "qrvault.db"
        private const val DB_VERSION = 1
        private const val TABLE = "qr_history"
        private const val COL_ID = "_id"
        private const val COL_NAME = "contact_name"
        private const val COL_PHONES = "phones"
        private const val COL_EMAILS = "emails"
        private const val COL_PAYLOAD = "payload"
        private const val COL_QR = "qr_image"
        private const val COL_CREATED = "created_at"
        private const val SEPARATOR = "\n"
    }
}
