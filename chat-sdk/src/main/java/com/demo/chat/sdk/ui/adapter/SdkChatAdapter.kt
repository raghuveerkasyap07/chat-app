package com.demo.chat.sdk.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.demo.chat.sdk.R
import com.demo.chat.sdk.core.model.SdkChatMessage
import com.demo.chat.sdk.core.model.SdkMessageStatus
import com.demo.chat.sdk.core.model.SdkMessageType
import com.demo.chat.sdk.databinding.SdkItemChatReceivedBinding
import com.demo.chat.sdk.databinding.SdkItemChatSentBinding
import com.demo.chat.sdk.databinding.SdkItemChatSystemBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SdkChatAdapter(
    private val onMessageLongClick: (SdkChatMessage) -> Unit
) : ListAdapter<SdkChatMessage, RecyclerView.ViewHolder>(SdkMessageDiffCallback()) {

    companion object {
        private const val TYPE_SENT = 0
        private const val TYPE_RECEIVED = 1
        private const val TYPE_SYSTEM = 2

        private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).type) {
            SdkMessageType.SENT -> TYPE_SENT
            SdkMessageType.RECEIVED -> TYPE_RECEIVED
            SdkMessageType.SYSTEM -> TYPE_SYSTEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SENT -> {
                val binding = SdkItemChatSentBinding.inflate(inflater, parent, false)
                SentViewHolder(binding)
            }
            TYPE_RECEIVED -> {
                val binding = SdkItemChatReceivedBinding.inflate(inflater, parent, false)
                ReceivedViewHolder(binding)
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
            is ReceivedViewHolder -> holder.bind(message)
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

    inner class SystemViewHolder(private val binding: SdkItemChatSystemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: SdkChatMessage) {
            binding.tvSystemText.text = message.text
        }
    }
}
