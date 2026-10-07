package com.demo.chat.sdk.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import com.demo.chat.sdk.core.model.SdkChatMessage

class SdkMessageDiffCallback : DiffUtil.ItemCallback<SdkChatMessage>() {
    override fun areItemsTheSame(oldItem: SdkChatMessage, newItem: SdkChatMessage): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SdkChatMessage, newItem: SdkChatMessage): Boolean {
        return oldItem == newItem
    }
}
