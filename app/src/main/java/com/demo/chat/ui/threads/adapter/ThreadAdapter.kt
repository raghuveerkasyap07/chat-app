package com.demo.chat.ui.threads.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.demo.chat.data.model.ChatThread
import com.demo.chat.databinding.ItemChatThreadBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ThreadAdapter(
    private val onThreadClick: (ChatThread) -> Unit
) : ListAdapter<ChatThread, ThreadAdapter.ThreadViewHolder>(ThreadDiffCallback()) {

    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadViewHolder {
        val binding = ItemChatThreadBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ThreadViewHolder(binding, onThreadClick, timeFormatter)
    }

    override fun onBindViewHolder(holder: ThreadViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ThreadViewHolder(
        private val binding: ItemChatThreadBinding,
        private val onThreadClick: (ChatThread) -> Unit,
        private val timeFormatter: SimpleDateFormat
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(thread: ChatThread) {
            val previewText = when {
                thread.lastMessageText.startsWith("http") || thread.lastMessageText.contains("[Photo]") -> "📷 Photo"
                else -> thread.lastMessageText
            }
            binding.tvLastMessage.text = previewText
            binding.tvTimestamp.text = timeFormatter.format(Date(thread.lastMessageTimestamp))

            // Generate initials
            val initials = thread.peerName.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
            binding.tvPeerInitials.text = if (initials.isNotEmpty()) initials else "CH"

            if (thread.unreadCount > 0) {
                binding.tvUnreadBadge.text = thread.unreadCount.toString()
                binding.tvUnreadBadge.visibility = View.VISIBLE
            } else {
                binding.tvUnreadBadge.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onThreadClick(thread)
            }
        }
    }
}
