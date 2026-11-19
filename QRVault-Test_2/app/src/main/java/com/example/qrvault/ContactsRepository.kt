package com.example.qrvault

import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data

/** One address-book entry: everything QRVault puts into a QR code. */
data class ContactInfo(
    val id: Long,
    val name: String,
    val phones: List<String>,
    val emails: List<String>
) {
    /** "First phone · first email", or null when the contact has neither. */
    val summary: String?
        get() = listOfNotNull(phones.firstOrNull(), emails.firstOrNull())
            .joinToString(" · ").ifEmpty { null }
}

/**
 * Reads contacts through the Contacts Content Provider via [android.content.ContentResolver].
 * Needs READ_CONTACTS. Call off the main thread.
 */
class ContactsRepository(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun loadAll(): List<ContactInfo> {
        // 1. Contacts table: one row per person, sorted by display name.
        val names = linkedMapOf<Long, String>()
        resolver.query(
            Contacts.CONTENT_URI,
            arrayOf(Contacts._ID, Contacts.DISPLAY_NAME_PRIMARY),
            null, null,
            "${Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(Contacts._ID)
            val nameCol = cursor.getColumnIndexOrThrow(Contacts.DISPLAY_NAME_PRIMARY)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameCol)?.trim().orEmpty()
                if (name.isNotEmpty()) names[cursor.getLong(idCol)] = name
            }
        }
        if (names.isEmpty()) return emptyList()

        // 2. Data table: every phone and email row in one query, grouped by contact ID.
        val phones = mutableMapOf<Long, MutableList<String>>()
        val emails = mutableMapOf<Long, MutableList<String>>()
        resolver.query(
            Data.CONTENT_URI,
            arrayOf(Data.CONTACT_ID, Data.MIMETYPE, Data.DATA1),
            "${Data.MIMETYPE} IN (?, ?)",
            arrayOf(Phone.CONTENT_ITEM_TYPE, Email.CONTENT_ITEM_TYPE),
            "${Data.IS_PRIMARY} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(Data.CONTACT_ID)
            val typeCol = cursor.getColumnIndexOrThrow(Data.MIMETYPE)
            val valueCol = cursor.getColumnIndexOrThrow(Data.DATA1)
            while (cursor.moveToNext()) {
                val value = cursor.getString(valueCol)?.trim()?.takeIf { it.isNotEmpty() } ?: continue
                val target = if (cursor.getString(typeCol) == Phone.CONTENT_ITEM_TYPE) phones else emails
                target.getOrPut(cursor.getLong(idCol)) { mutableListOf() }.add(value)
            }
        }

        return names.map { (id, name) ->
            ContactInfo(
                id = id,
                name = name,
                // The same number often appears twice with different spacing (e.g. synced from two accounts).
                phones = phones[id].orEmpty().distinctBy { it.filter { c -> c.isDigit() || c == '+' } },
                emails = emails[id].orEmpty().distinctBy { it.lowercase() }
            )
        }
    }
}
