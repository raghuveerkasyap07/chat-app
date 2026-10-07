package com.demo.chat.ui.chat

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.demo.chat.DemoChatApplication
import com.demo.chat.R
import com.demo.chat.data.model.ChatMessage
import com.demo.chat.data.model.ConnectionState
import com.demo.chat.databinding.ActivityChatBinding
import com.demo.chat.databinding.DialogImagePreviewBinding
import com.demo.chat.databinding.DialogServerConfigBinding
import com.demo.chat.ui.chat.adapter.ChatAdapter
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CHAT_ID = "extra_chat_id"
        const val EXTRA_THREAD_ID = "extra_thread_id"
        const val EXTRA_PARTNER_NAME = "extra_partner_name"
        const val EXTRA_PEER_NAME = "extra_peer_name"
    }

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter

    private var chatId: String = "chat_echo_bot"
    private var partnerName: String = "Echo Bot"

    private val viewModel: ChatViewModel by viewModels {
        val app = application as DemoChatApplication
        ChatViewModelFactory(
            webSocketManager = app.webSocketManager,
            chatRepository = app.chatRepository,
            mediaRepository = app.mediaRepository,
            sessionManager = app.sessionManager
        )
    }

    // System Photo Picker launcher
    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                viewModel.sendPhoto(uri, this)
            } else {
                Toast.makeText(this, "No photo selected", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatId = intent.getStringExtra(EXTRA_CHAT_ID)
            ?: intent.getStringExtra(EXTRA_THREAD_ID)
            ?: "chat_echo_bot"

        partnerName = intent.getStringExtra(EXTRA_PARTNER_NAME)
            ?: intent.getStringExtra(EXTRA_PEER_NAME)
            ?: "Echo Bot"

        viewModel.initChat(chatId, partnerName)

        setupToolbar()
        setupRecyclerView()
        setupInputAndChips()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.tvToolbarTitle.text = partnerName
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(
            onMessageLongClick = { message ->
                copyMessageToClipboard(message)
            },
            onImageClick = { imageUrl ->
                showFullscreenImagePreview(imageUrl)
            }
        )
        binding.rvChatMessages.adapter = chatAdapter
    }

    private fun setupInputAndChips() {
        binding.etMessage.addTextChangedListener { text ->
            val hasText = !text.isNullOrBlank()
            val isConnected = viewModel.connectionState.value is ConnectionState.Connected
            binding.btnSend.isEnabled = hasText && isConnected
            binding.btnSend.alpha = if (hasText && isConnected) 1.0f else 0.5f
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.etMessage.text?.clear()
            }
        }

        // Photo Picker trigger via attachment button
        binding.btnAttach.setOnClickListener {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.chipHello.setOnClickListener {
            viewModel.sendMessage("👋 Hello! Connecting from Demo Chat Android App.")
        }

        binding.chipEcho.setOnClickListener {
            viewModel.sendMessage("📡 Echo Test: 1, 2, 3... WebSocket connection active!")
        }

        binding.chipPing.setOnClickListener {
            viewModel.sendPing()
        }

        binding.chipHowAreYou.setOnClickListener {
            viewModel.sendMessage("💬 How are you, WebSocket Echo Server?")
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.messages.collect { messages ->
                        chatAdapter.submitList(messages) {
                            if (messages.isNotEmpty()) {
                                binding.rvChatMessages.smoothScrollToPosition(messages.size - 1)
                            }
                        }
                    }
                }

                launch {
                    viewModel.connectionState.collect { state ->
                        updateConnectionUi(state)
                    }
                }
            }
        }
    }

    private fun updateConnectionUi(state: ConnectionState) {
        val isConnected = state is ConnectionState.Connected
        val hasText = !binding.etMessage.text.isNullOrBlank()

        binding.btnSend.isEnabled = isConnected && hasText
        binding.btnSend.alpha = if (isConnected && hasText) 1.0f else 0.5f

        when (state) {
            is ConnectionState.Connected -> {
                binding.viewStatusDot.setBackgroundResource(R.drawable.bg_status_dot_connected)
                binding.tvStatusText.text = getString(R.string.status_connected)
            }
            is ConnectionState.Connecting -> {
                binding.viewStatusDot.setBackgroundResource(R.drawable.bg_status_dot_connecting)
                binding.tvStatusText.text = getString(R.string.status_connecting)
            }
            is ConnectionState.Disconnected -> {
                binding.viewStatusDot.setBackgroundResource(R.drawable.bg_status_dot_disconnected)
                binding.tvStatusText.text = getString(R.string.status_disconnected)
            }
            is ConnectionState.Error -> {
                binding.viewStatusDot.setBackgroundResource(R.drawable.bg_status_dot_disconnected)
                binding.tvStatusText.text = getString(R.string.status_disconnected)
            }
        }
    }

    // Fullscreen image preview dialog
    private fun showFullscreenImagePreview(imageUrl: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val previewBinding = DialogImagePreviewBinding.inflate(layoutInflater)
        dialog.setContentView(previewBinding.root)

        previewBinding.ivFullscreenImage.load(imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_photo)
            error(R.drawable.ic_error)
        }

        previewBinding.btnClosePreview.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun copyMessageToClipboard(message: ChatMessage) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val textToCopy = message.imageUrl ?: message.text
        val clip = ClipData.newPlainText("Chat Message", textToCopy)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.msg_copied, Toast.LENGTH_SHORT).show()
    }

    private fun showServerConfigDialog() {
        val dialogBinding = DialogServerConfigBinding.inflate(layoutInflater)
        dialogBinding.etServerUrl.setText(viewModel.getServerUrl())

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_server_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_connect) { _, _ ->
                val newUrl = dialogBinding.etServerUrl.text.toString().trim()
                if (newUrl.isNotBlank()) {
                    viewModel.connect(newUrl)
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_ping -> {
                viewModel.sendPing()
                true
            }
            R.id.action_reconnect -> {
                viewModel.reconnect()
                true
            }
            R.id.action_server_config -> {
                showServerConfigDialog()
                true
            }
            R.id.action_clear_chat -> {
                viewModel.clearChat()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
