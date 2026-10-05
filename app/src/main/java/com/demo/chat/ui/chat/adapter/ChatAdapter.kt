package com.demo.chat.ui.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.demo.chat.R
import com.demo.chat.data.model.ChatMessage
import com.demo.chat.data.model.MessageStatus
import com.demo.chat.data.model.MessageType
import com.demo.chat.databinding.ItemChatReceivedBinding
import com.demo.chat.databinding.ItemChatSentBinding
import com.demo.chat.databinding.ItemChatSystemBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val onMessageLongClick: (ChatMessage) -> Unit
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatMessageDiffCallback()) {

    companion object {
        private const val TYPE_SENT = 0
        private const val TYPE_RECEIVED = 1
        private const val TYPE_SYSTEM = 2

        private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).type) {
            MessageType.SENT -> TYPE_SENT
            MessageType.RECEIVED -> TYPE_RECEIVED
            MessageType.SYSTEM -> TYPE_SYSTEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SENT -> {
                val binding = ItemChatSentBinding.inflate(inflater, parent, false)
                SentViewHolder(binding)
            }
            TYPE_RECEIVED -> {
                val binding = ItemChatReceivedBinding.inflate(inflater, parent, false)
                ReceivedViewHolder(binding)
            }
            else -> {
                val binding = ItemChatSystemBinding.inflate(inflater, parent, false)
                SystemViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentViewHolder -> holder.bind(message)
            is ReceivedViewHolder -> holder.bind(message)
            is SystemViewHolder -> holder.bind(message)
        }
    }

    inner class SentViewHolder(private val binding: ItemChatSentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.tvSentText.text = message.text
            binding.tvSentTime.text = timeFormatter.format(Date(message.timestamp))

            when (message.status) {
                MessageStatus.SENDING -> binding.ivSentStatus.setImageResource(R.drawable.ic_check)
                MessageStatus.SENT -> binding.ivSentStatus.setImageResource(R.drawable.ic_double_check)
                MessageStatus.FAILED -> binding.ivSentStatus.setImageResource(R.drawable.ic_error)
            }

            binding.root.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }

    inner class ReceivedViewHolder(private val binding: ItemChatReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.tvSenderName.text = message.sender
            binding.tvReceivedText.text = message.text
            binding.tvReceivedTime.text = timeFormatter.format(Date(message.timestamp))

            binding.root.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }

    inner class SystemViewHolder(private val binding: ItemChatSystemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.tvSystemText.text = message.text
        }
    }
}
