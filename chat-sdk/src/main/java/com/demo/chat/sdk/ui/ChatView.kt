package com.demo.chat.sdk.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import coil.load
import com.demo.chat.sdk.ChatEngine
import com.demo.chat.sdk.R
import com.demo.chat.sdk.core.ChatClient
import com.demo.chat.sdk.core.model.SdkChatMessage
import com.demo.chat.sdk.core.model.SdkConnectionState
import com.demo.chat.sdk.core.model.SdkMessageStatus
import com.demo.chat.sdk.core.model.SdkMessageType
import com.demo.chat.sdk.databinding.SdkChatViewBinding
import com.demo.chat.sdk.databinding.SdkDialogImagePreviewBinding
import com.demo.chat.sdk.ui.adapter.SdkChatAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ChatView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: SdkChatViewBinding =
        SdkChatViewBinding.inflate(LayoutInflater.from(context), this, true)

    private val chatAdapter = SdkChatAdapter(
        onMessageLongClick = { message -> copyToClipboard(message.text) },
        onImageClick = { imageUrl -> showImagePreviewDialog(imageUrl) }
    )

    private var chatClient: ChatClient? = null
    private val messages = mutableListOf<SdkChatMessage>()
    private val viewScope = CoroutineScope(Dispatchers.Main + Job())

    var onAttachmentClickListener: (() -> Unit)? = null

    init {
        setupAttrs(attrs, defStyleAttr)
        setupRecyclerView()
        setupInputAndChips()

        if (ChatEngine.isInitialized()) {
            setupWithEngine(ChatEngine.getInstance())
        }
    }

    private fun setupAttrs(attrs: AttributeSet?, defStyleAttr: Int) {
        if (attrs == null) return
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ChatView, defStyleAttr, 0)
        try {
            val title = typedArray.getString(R.styleable.ChatView_titleText)
            if (!title.isNullOrBlank()) {
                binding.tvSdkHeaderTitle.text = title
            }

            val showSuggestions = typedArray.getBoolean(R.styleable.ChatView_showSuggestions, true)
            binding.suggestionsScroll.visibility = if (showSuggestions) View.VISIBLE else View.GONE
        } finally {
            typedArray.recycle()
        }
    }

    private fun setupRecyclerView() {
        binding.rvSdkChatMessages.adapter = chatAdapter
    }

    private fun setupInputAndChips() {
        binding.btnSdkAttach.setOnClickListener {
            onAttachmentClickListener?.invoke()
        }

        binding.etSdkMessage.addTextChangedListener { text ->
            val hasText = !text.isNullOrBlank()
            val isConnected = chatClient?.connectionState?.value is SdkConnectionState.Connected
            binding.btnSdkSend.isEnabled = hasText && isConnected
            binding.btnSdkSend.alpha = if (hasText && isConnected) 1.0f else 0.5f
        }

        binding.btnSdkSend.setOnClickListener {
            val text = binding.etSdkMessage.text.toString().trim()
            if (text.isNotBlank()) {
                sendMessage(text)
                binding.etSdkMessage.text?.clear()
            }
        }

        binding.chipHello.setOnClickListener {
            sendMessage("👋 Hello from Chat SDK Integration!")
        }

        binding.chipEcho.setOnClickListener {
            sendMessage("🧪 SDK Echo Test: Real-time WebSocket connection!")
        }

        binding.chipPing.setOnClickListener {
            val sent = chatClient?.sendPing() ?: false
            if (sent) {
                addSystemMessage("Ping sent via SDK")
            }
        }

        binding.chipHowAreYou.setOnClickListener {
            sendMessage("❓ How are you, WebSocket Echo Server?")
        }
    }

    fun setupWithEngine(chatEngine: ChatEngine) {
        this.chatClient = chatEngine.client
        observeClient(chatEngine.client)
        chatEngine.client.connect()
    }

    private fun observeClient(client: ChatClient) {
        viewScope.launch {
            launch {
                client.connectionState.collect { state ->
                    updateConnectionUi(state)
                }
            }

            launch {
                client.incomingMessages.collect { text ->
                    if (text == "PING_TEST") {
                        addSystemMessage("Pong response received via SDK! 🏓")
                    } else if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("content://")) {
                        val imgMsg = SdkChatMessage(
                            imageUrl = text,
                            type = SdkMessageType.RECEIVED,
                            sender = "Echo Bot"
                        )
                        addMessage(imgMsg)
                    } else {
                        val msg = SdkChatMessage(
                            text = text,
                            type = SdkMessageType.RECEIVED,
                            sender = "Echo Bot"
                        )
                        addMessage(msg)
                    }
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val client = chatClient ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val message = SdkChatMessage(
            text = trimmed,
            type = SdkMessageType.SENT,
            status = SdkMessageStatus.SENDING
        )

        addMessage(message)

        val isSent = client.sendMessage(trimmed)
        val updatedStatus = if (isSent) SdkMessageStatus.SENT else SdkMessageStatus.FAILED
        updateMessageStatus(message.id, updatedStatus)
    }

    fun sendImageMessage(imageUrl: String, caption: String = "") {
        val client = chatClient ?: return

        val message = SdkChatMessage(
            text = caption,
            imageUrl = imageUrl,
            type = SdkMessageType.SENT,
            status = SdkMessageStatus.SENDING
        )

        addMessage(message)

        val isSent = client.sendMessage(imageUrl)
        val updatedStatus = if (isSent) SdkMessageStatus.SENT else SdkMessageStatus.FAILED
        updateMessageStatus(message.id, updatedStatus)
    }

    private fun addMessage(message: SdkChatMessage) {
        messages.add(message)
        chatAdapter.submitList(messages.toList()) {
            if (messages.isNotEmpty()) {
                binding.rvSdkChatMessages.smoothScrollToPosition(messages.size - 1)
            }
        }
    }

    private fun addSystemMessage(text: String) {
        val sysMsg = SdkChatMessage(
            text = text,
            type = SdkMessageType.SYSTEM,
            sender = "System"
        )
        addMessage(sysMsg)
    }

    private fun updateMessageStatus(id: String, status: SdkMessageStatus) {
        val index = messages.indexOfFirst { it.id == id }
        if (index != -1) {
            messages[index] = messages[index].copy(status = status)
            chatAdapter.submitList(messages.toList())
        }
    }

    private fun updateConnectionUi(state: SdkConnectionState) {
        val isConnected = state is SdkConnectionState.Connected
        val hasText = !binding.etSdkMessage.text.isNullOrBlank()

        binding.btnSdkSend.isEnabled = isConnected && hasText
        binding.btnSdkSend.alpha = if (isConnected && hasText) 1.0f else 0.5f

        when (state) {
            is SdkConnectionState.Connected -> {
                binding.viewSdkStatusDot.setBackgroundResource(R.drawable.sdk_bg_status_dot_connected)
                binding.tvSdkStatusText.text = context.getString(R.string.sdk_status_connected)
                addSystemMessage("Connected to WebSocket Server (${chatClient?.getCurrentUrl()})")
            }
            is SdkConnectionState.Connecting -> {
                binding.viewSdkStatusDot.setBackgroundResource(R.drawable.sdk_bg_status_dot_connecting)
                binding.tvSdkStatusText.text = context.getString(R.string.sdk_status_connecting)
            }
            is SdkConnectionState.Disconnected -> {
                binding.viewSdkStatusDot.setBackgroundResource(R.drawable.sdk_bg_status_dot_disconnected)
                binding.tvSdkStatusText.text = context.getString(R.string.sdk_status_disconnected)
            }
            is SdkConnectionState.Error -> {
                binding.viewSdkStatusDot.setBackgroundResource(R.drawable.sdk_bg_status_dot_disconnected)
                binding.tvSdkStatusText.text = context.getString(R.string.sdk_status_disconnected)
                addSystemMessage("Connection Error: ${state.message}")
            }
        }
    }

    private fun showImagePreviewDialog(imageUrl: String) {
        val dialogBinding = SdkDialogImagePreviewBinding.inflate(LayoutInflater.from(context))
        dialogBinding.ivFullImage.load(imageUrl) {
            crossfade(true)
            placeholder(R.drawable.sdk_ic_photo)
            error(R.drawable.sdk_ic_error)
        }

        val dialog = AlertDialog.Builder(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.root.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun copyToClipboard(text: String) {
        if (text.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SDK Chat Message", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, R.string.sdk_msg_copied, Toast.LENGTH_SHORT).show()
    }
}
