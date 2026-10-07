package com.demo.chat.sdk.core

import com.demo.chat.sdk.core.model.SdkChatMessage
import com.demo.chat.sdk.core.model.SdkConnectionState

fun interface ConnectionListener {
    fun onConnectionStateChanged(state: SdkConnectionState)
}

fun interface MessageListener {
    fun onMessageReceived(message: SdkChatMessage)
}
