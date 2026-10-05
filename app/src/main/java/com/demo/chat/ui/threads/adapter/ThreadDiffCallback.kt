package com.demo.chat.ui.threads.adapter

import androidx.recyclerview.widget.DiffUtil
import com.demo.chat.data.model.ChatThread

class ThreadDiffCallback : DiffUtil.ItemCallback<ChatThread>() {
    override fun areItemsTheSame(oldItem: ChatThread, newItem: ChatThread): Boolean {
        return oldItem.threadId == newItem.threadId
    }

    override fun areContentsTheSame(oldItem: ChatThread, newItem: ChatThread): Boolean {
        return oldItem == newItem
    }
}
