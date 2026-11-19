package com.example.qrvault

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/** RecyclerView adapter for the device contact list. Tapping a row selects that contact. */
class ContactAdapter(
    private val onSelect: (ContactInfo) -> Unit
) : ListAdapter<ContactInfo, ContactAdapter.Holder>(Diff) {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: TextView = view.findViewById(R.id.avatar)
        val name: TextView = view.findViewById(R.id.name)
        val summary: TextView = view.findViewById(R.id.summary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val contact = getItem(position)
        holder.avatar.bindAvatar(contact.name)
        holder.name.text = contact.name
        holder.summary.text = contact.summary ?: holder.itemView.context.getString(R.string.no_details)
        holder.itemView.setOnClickListener { onSelect(contact) }
    }

    private object Diff : DiffUtil.ItemCallback<ContactInfo>() {
        override fun areItemsTheSame(old: ContactInfo, new: ContactInfo) = old.id == new.id
        override fun areContentsTheSame(old: ContactInfo, new: ContactInfo) = old == new
    }
}
