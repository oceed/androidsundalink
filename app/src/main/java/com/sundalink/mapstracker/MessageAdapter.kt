package com.sundalink.mapstracker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val context: Context, private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = if (viewType == 0) {
            LayoutInflater.from(context).inflate(R.layout.item_message_self, parent, false)
        } else {
            LayoutInflater.from(context).inflate(R.layout.item_message_other, parent, false)
        }
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.messageText.text = message.message
        holder.senderName.text = message.senderName

        if (message.isSelf) {
            holder.senderName.visibility = View.GONE // Sembunyikan nama pengirim
        } else {
            holder.senderName.text = message.senderName
            holder.senderName.visibility = View.VISIBLE // Tampilkan nama pengirim
        }

        val formattedTime = formatTimestamp(message.timestamp)
        holder.timestampText.text = formattedTime
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isSelf) 0 else 1
    }

    private fun formatTimestamp(timestamp: String): String {
        val formatter = java.time.format.DateTimeFormatter.ISO_DATE_TIME
        val dateTime = java.time.ZonedDateTime.parse(timestamp, formatter)
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }

    override fun getItemCount() = messages.size

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
        val senderName: TextView = view.findViewById(R.id.senderName)
        val timestampText: TextView = view.findViewById(R.id.timestampText)
    }
}
