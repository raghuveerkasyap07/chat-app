package com.demo.chat.ui.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.demo.chat.R
import com.demo.chat.data.model.ChatMessage
import com.demo.chat.data.model.MessageStatus
import com.demo.chat.data.model.MessageType
import com.demo.chat.databinding.ItemChatReceivedBinding
import com.demo.chat.databinding.ItemChatReceivedImageBinding
import com.demo.chat.databinding.ItemChatSentBinding
import com.demo.chat.databinding.ItemChatSentImageBinding
import com.demo.chat.databinding.ItemChatSystemBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val onMessageLongClick: (ChatMessage) -> Unit = {},
    private val onImageClick: (String) -> Unit = {}
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatMessageDiffCallback()) {

    companion object {
        const val TYPE_SENT_TEXT = 0
        const val TYPE_SENT_IMAGE = 1
        const val TYPE_RECEIVED_TEXT = 2
        const val TYPE_RECEIVED_IMAGE = 3
        const val TYPE_SYSTEM = 4

        private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return when (message.type) {
            MessageType.SENT_TEXT -> TYPE_SENT_TEXT
            MessageType.SENT_IMAGE -> TYPE_SENT_IMAGE
            MessageType.RECEIVED_TEXT -> TYPE_RECEIVED_TEXT
            MessageType.RECEIVED_IMAGE -> TYPE_RECEIVED_IMAGE
            MessageType.SENT -> if (!message.imageUrl.isNullOrBlank()) TYPE_SENT_IMAGE else TYPE_SENT_TEXT
            MessageType.RECEIVED -> if (!message.imageUrl.isNullOrBlank()) TYPE_RECEIVED_IMAGE else TYPE_RECEIVED_TEXT
            MessageType.SYSTEM -> TYPE_SYSTEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SENT_TEXT -> SentTextViewHolder(ItemChatSentBinding.inflate(inflater, parent, false))
            TYPE_SENT_IMAGE -> SentImageViewHolder(ItemChatSentImageBinding.inflate(inflater, parent, false))
            TYPE_RECEIVED_TEXT -> ReceivedTextViewHolder(ItemChatReceivedBinding.inflate(inflater, parent, false))
            TYPE_RECEIVED_IMAGE -> ReceivedImageViewHolder(ItemChatReceivedImageBinding.inflate(inflater, parent, false))
            else -> SystemViewHolder(ItemChatSystemBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentTextViewHolder -> holder.bind(message)
            is SentImageViewHolder -> holder.bind(message)
            is ReceivedTextViewHolder -> holder.bind(message)
            is ReceivedImageViewHolder -> holder.bind(message)
            is SystemViewHolder -> holder.bind(message)
        }
    }

    inner class SentTextViewHolder(private val binding: ItemChatSentBinding) :
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

    inner class SentImageViewHolder(private val binding: ItemChatSentImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            val url = message.imageUrl ?: message.text
            binding.ivSentImage.load(url) {
                crossfade(true)
                placeholder(R.drawable.ic_photo)
                error(R.drawable.ic_error)
            }
            binding.tvSentImageTime.text = timeFormatter.format(Date(message.timestamp))

            when (message.status) {
                MessageStatus.SENDING -> binding.ivSentImageStatus.setImageResource(R.drawable.ic_check)
                MessageStatus.SENT -> binding.ivSentImageStatus.setImageResource(R.drawable.ic_double_check)
                MessageStatus.FAILED -> binding.ivSentImageStatus.setImageResource(R.drawable.ic_error)
            }

            binding.ivSentImage.setOnClickListener {
                if (!url.isNullOrBlank()) {
                    onImageClick(url)
                }
            }

            binding.root.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }

    inner class ReceivedTextViewHolder(private val binding: ItemChatReceivedBinding) :
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

    inner class ReceivedImageViewHolder(private val binding: ItemChatReceivedImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.tvReceivedImageSender.text = message.sender
            val url = message.imageUrl ?: message.text
            binding.ivReceivedImage.load(url) {
                crossfade(true)
                placeholder(R.drawable.ic_photo)
                error(R.drawable.ic_error)
            }
            binding.tvReceivedImageTime.text = timeFormatter.format(Date(message.timestamp))

            binding.ivReceivedImage.setOnClickListener {
                if (!url.isNullOrBlank()) {
                    onImageClick(url)
                }
            }

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
