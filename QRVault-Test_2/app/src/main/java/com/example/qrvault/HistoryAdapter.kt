package com.example.qrvault

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/** A history entry with its QR thumbnail already decoded (done off the main thread). */
class HistoryRow(val entry: HistoryEntry, val thumbnail: Bitmap?)

class HistoryAdapter(
    private val onOpen: (HistoryEntry) -> Unit,
    private val onDelete: (HistoryEntry) -> Unit
) : ListAdapter<HistoryRow, HistoryAdapter.Holder>(Diff) {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val thumb: ImageView = view.findViewById(R.id.thumb)
        val name: TextView = view.findViewById(R.id.name)
        val summary: TextView = view.findViewById(R.id.summary)
        val date: TextView = view.findViewById(R.id.date)
        val delete: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        val entry = row.entry
        val context = holder.itemView.context
        holder.thumb.setImageBitmap(row.thumbnail)
        holder.thumb.contentDescription = context.getString(R.string.cd_qr, entry.contactName)
        holder.name.text = entry.contactName
        holder.summary.text = listOfNotNull(entry.phones.firstOrNull(), entry.emails.firstOrNull())
            .joinToString(" · ").ifEmpty { context.getString(R.string.no_details) }
        holder.date.text = context.formatTimestamp(entry.createdAt)
        holder.itemView.setOnClickListener { onOpen(entry) }
        holder.delete.setOnClickListener { onDelete(entry) }
    }

    private object Diff : DiffUtil.ItemCallback<HistoryRow>() {
        override fun areItemsTheSame(old: HistoryRow, new: HistoryRow) = old.entry.id == new.entry.id
        override fun areContentsTheSame(old: HistoryRow, new: HistoryRow) =
            old.entry.createdAt == new.entry.createdAt && old.entry.contactName == new.entry.contactName
    }
}
