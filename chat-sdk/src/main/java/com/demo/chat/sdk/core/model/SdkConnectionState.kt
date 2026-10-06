package com.demo.chat.sdk.core.model

sealed class SdkConnectionState {
    data object Disconnected : SdkConnectionState()
    data object Connecting : SdkConnectionState()
    data object Connected : SdkConnectionState()
    data class Error(val message: String) : SdkConnectionState()
}
