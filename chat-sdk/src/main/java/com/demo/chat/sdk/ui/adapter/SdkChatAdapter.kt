package com.demo.chat.sdk.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.demo.chat.sdk.R
import com.demo.chat.sdk.core.model.SdkChatMessage
import com.demo.chat.sdk.core.model.SdkMessageStatus
import com.demo.chat.sdk.core.model.SdkMessageType
import com.demo.chat.sdk.databinding.SdkItemChatReceivedBinding
import com.demo.chat.sdk.databinding.SdkItemChatReceivedImageBinding
import com.demo.chat.sdk.databinding.SdkItemChatSentBinding
import com.demo.chat.sdk.databinding.SdkItemChatSentImageBinding
import com.demo.chat.sdk.databinding.SdkItemChatSystemBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SdkChatAdapter(
    private val onMessageLongClick: (SdkChatMessage) -> Unit,
    private val onImageClick: (String) -> Unit
) : ListAdapter<SdkChatMessage, RecyclerView.ViewHolder>(SdkMessageDiffCallback()) {

    companion object {
        private const val TYPE_SENT_TEXT = 0
        private const val TYPE_RECEIVED_TEXT = 1
        private const val TYPE_SYSTEM = 2
        private const val TYPE_SENT_IMAGE = 3
        private const val TYPE_RECEIVED_IMAGE = 4

        private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return when {
            message.type == SdkMessageType.SYSTEM -> TYPE_SYSTEM
            message.type == SdkMessageType.SENT && message.isImage -> TYPE_SENT_IMAGE
            message.type == SdkMessageType.SENT -> TYPE_SENT_TEXT
            message.type == SdkMessageType.RECEIVED && message.isImage -> TYPE_RECEIVED_IMAGE
            else -> TYPE_RECEIVED_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SENT_TEXT -> {
                val binding = SdkItemChatSentBinding.inflate(inflater, parent, false)
                SentViewHolder(binding)
            }
            TYPE_SENT_IMAGE -> {
                val binding = SdkItemChatSentImageBinding.inflate(inflater, parent, false)
                SentImageViewHolder(binding)
            }
            TYPE_RECEIVED_TEXT -> {
                val binding = SdkItemChatReceivedBinding.inflate(inflater, parent, false)
                ReceivedViewHolder(binding)
            }
            TYPE_RECEIVED_IMAGE -> {
                val binding = SdkItemChatReceivedImageBinding.inflate(inflater, parent, false)
                ReceivedImageViewHolder(binding)
            }
            else -> {
                val binding = SdkItemChatSystemBinding.inflate(inflater, parent, false)
                SystemViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentViewHolder -> holder.bind(message)
            is SentImageViewHolder -> holder.bind(message)
            is ReceivedViewHolder -> holder.bind(message)
            is ReceivedImageViewHolder -> holder.bind(message)
            is SystemViewHolder -> holder.bind(message)
        }
    }

    inner class SentViewHolder(private val binding: SdkItemChatSentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: SdkChatMessage) {
            binding.tvSentText.text = message.text
            binding.tvSentTime.text = timeFormatter.format(Date(message.timestamp))

            when (message.status) {
                SdkMessageStatus.SENDING -> binding.ivSentStatus.setImageResource(R.drawable.sdk_ic_check)
                SdkMessageStatus.SENT -> binding.ivSentStatus.setImageResource(R.drawable.sdk_ic_double_check)
                SdkMessageStatus.FAILED -> binding.ivSentStatus.setImageResource(R.drawable.sdk_ic_error)
            }

            binding.root.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }

    inner class SentImageViewHolder(private val binding: SdkItemChatSentImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: SdkChatMessage) {
            binding.tvSentImageTime.text = timeFormatter.format(Date(message.timestamp))
            message.imageUrl?.let { url ->
                binding.ivSentImage.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.sdk_ic_photo)
                    error(R.drawable.sdk_ic_error)
                }
                binding.ivSentImage.setOnClickListener { onImageClick(url) }
            }

            if (!message.text.isNullOrBlank()) {
                binding.tvSentImageCaption.text = message.text
                binding.tvSentImageCaption.visibility = View.VISIBLE
            } else {
                binding.tvSentImageCaption.visibility = View.GONE
            }

            when (message.status) {
                SdkMessageStatus.SENDING -> binding.ivSentImageStatus.setImageResource(R.drawable.sdk_ic_check)
                SdkMessageStatus.SENT -> binding.ivSentImageStatus.setImageResource(R.drawable.sdk_ic_double_check)
                SdkMessageStatus.FAILED -> binding.ivSentImageStatus.setImageResource(R.drawable.sdk_ic_error)
            }

            binding.root.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }

    inner class ReceivedViewHolder(private val binding: SdkItemChatReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: SdkChatMessage) {
            binding.tvSenderName.text = message.sender
            binding.tvReceivedText.text = message.text
            binding.tvReceivedTime.text = timeFormatter.format(Date(message.timestamp))

            binding.root.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }

    inner class ReceivedImageViewHolder(private val binding: SdkItemChatReceivedImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: SdkChatMessage) {
            binding.tvSenderName.text = message.sender
            binding.tvReceivedImageTime.text = timeFormatter.format(Date(message.timestamp))
            message.imageUrl?.let { url ->
                binding.ivReceivedImage.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.sdk_ic_photo)
                    error(R.drawable.sdk_ic_error)
                }
                binding.ivReceivedImage.setOnClickListener { onImageClick(url) }
            }

            if (!message.text.isNullOrBlank()) {
                binding.tvReceivedImageCaption.text = message.text
                binding.tvReceivedImageCaption.visibility = View.VISIBLE
            } else {
                binding.tvReceivedImageCaption.visibility = View.GONE
            }

            binding.root.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }

    inner class SystemViewHolder(private val binding: SdkItemChatSystemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: SdkChatMessage) {
            binding.tvSystemText.text = message.text
        }
    }
}
