package com.demo.chat.ui.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.demo.chat.DemoChatApplication
import com.demo.chat.R
import com.demo.chat.data.model.ChatMessage
import com.demo.chat.data.model.ConnectionState
import com.demo.chat.data.model.MessageType
import com.demo.chat.databinding.ActivityChatBinding
import com.demo.chat.databinding.DialogServerConfigBinding
import com.demo.chat.ui.chat.adapter.ChatAdapter
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_THREAD_ID = "extra_thread_id"
        const val EXTRA_PEER_NAME = "extra_peer_name"
    }

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private var peerName: String = "Echo Bot"
    private var threadId: String = "thread_echo_bot"
    private var lastNotifiedMessageId: String? = null

    private val viewModel: ChatViewModel by viewModels {
        val app = application as DemoChatApplication
        ChatViewModelFactory(app.webSocketManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        threadId = intent.getStringExtra(EXTRA_THREAD_ID) ?: "thread_echo_bot"
        peerName = intent.getStringExtra(EXTRA_PEER_NAME) ?: "Echo Bot"

        setupToolbar()
        setupRecyclerView()
        setupInputAndChips()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.tvToolbarTitle.text = peerName
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(
            onMessageLongClick = { message ->
                copyMessageToClipboard(message)
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

        binding.chipHello.setOnClickListener {
            viewModel.sendMessage("👋 Hello! Connecting from Demo Chat Android App.")
        }

        binding.chipEcho.setOnClickListener {
            viewModel.sendMessage("🧪 Echo Test: 1, 2, 3... WebSocket connection active!")
        }

        binding.chipPing.setOnClickListener {
            viewModel.sendPing()
        }

        binding.chipHowAreYou.setOnClickListener {
            viewModel.sendMessage("❓ How are you, WebSocket Echo Server?")
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

                        // Trigger notification for incoming received messages
                        val lastMessage = messages.lastOrNull()
                        if (lastMessage != null &&
                            lastMessage.type == MessageType.RECEIVED &&
                            lastMessage.id != lastNotifiedMessageId
                        ) {
                            lastNotifiedMessageId = lastMessage.id
                            showIncomingMessageNotification(lastMessage.sender, lastMessage.text)
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

    private fun showIncomingMessageNotification(sender: String, text: String) {
        val channelId = "chat_notifications_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Chat Notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra(EXTRA_THREAD_ID, threadId)
            putExtra(EXTRA_PEER_NAME, peerName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            threadId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_websocket)
            .setContentTitle("New message from $sender")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        try {
            with(NotificationManagerCompat.from(this)) {
                notify(text.hashCode(), builder.build())
            }
        } catch (_: SecurityException) {
            // Permission not granted
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

    private fun copyMessageToClipboard(message: ChatMessage) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Chat Message", message.text)
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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showTestNotification()
        } else {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndShowTestNotification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                showTestNotification()
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            showTestNotification()
        }
    }

    private fun showTestNotification() {
        val channelId = "chat_notifications_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Chat Notifications"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel(channelId, name, importance)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra(EXTRA_THREAD_ID, threadId)
            putExtra(EXTRA_PEER_NAME, peerName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            threadId.hashCode(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_websocket)
            .setContentTitle("New message from $peerName")
            .setContentText("Local test notification: Tap to open chat!")
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        try {
            with(androidx.core.app.NotificationManagerCompat.from(this)) {
                notify(2002, builder.build())
            }
            Toast.makeText(this, "Local notification sent!", Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, "Notification permission required", Toast.LENGTH_SHORT).show()
        }
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
            R.id.action_test_notification -> {
                checkAndShowTestNotification()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
