package com.demo.chat.ui.chat

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.demo.chat.databinding.ActivityChatBinding
import com.demo.chat.sdk.ChatEngine

class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_THREAD_ID = "EXTRA_THREAD_ID"
        const val EXTRA_PEER_NAME = "EXTRA_PEER_NAME"
    }

    private lateinit var binding: ActivityChatBinding

    // System Photo Picker launcher
    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                binding.chatView.sendImageMessage(uri.toString(), caption = "Photo attachment")
            } else {
                Toast.makeText(this, "No photo selected", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val peerName = intent.getStringExtra(EXTRA_PEER_NAME) ?: "Chat Room"

        // 1. Initialize Chat SDK Engine
        val chatEngine = ChatEngine.initialize(
            context = this,
            serverUrl = ChatEngine.DEFAULT_SERVER_URL
        )

        // 2. Setup the Chat UI View with the Engine & Header Title
        binding.chatView.setupWithEngine(chatEngine)
        binding.chatView.setHeaderTitle(peerName)

        // 3. Attach Photo Picker Click Listener
        binding.chatView.onAttachmentClickListener = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }
}
