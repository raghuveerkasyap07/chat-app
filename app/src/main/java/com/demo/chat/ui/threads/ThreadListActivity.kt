package com.demo.chat.ui.threads

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.demo.chat.databinding.ActivityThreadListBinding
import com.demo.chat.data.model.ThreadListUiState
import com.demo.chat.ui.chat.ChatActivity
import com.demo.chat.ui.threads.adapter.ThreadAdapter
import kotlinx.coroutines.launch

class ThreadListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThreadListBinding
    private lateinit var threadAdapter: ThreadAdapter
    private val viewModel: ThreadListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThreadListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Chats"
    }

    private fun setupRecyclerView() {
        threadAdapter = ThreadAdapter { thread ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_THREAD_ID, thread.threadId)
                putExtra(ChatActivity.EXTRA_PEER_NAME, thread.peerName)
            }
            startActivity(intent)
        }
        binding.rvThreads.adapter = threadAdapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.threads.collect { threads ->
                        threadAdapter.submitList(threads)
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is ThreadListUiState.Loading -> {
                                binding.tvEmptyState.visibility = View.GONE
                            }
                            is ThreadListUiState.Success -> {
                                binding.tvEmptyState.visibility =
                                    if (state.threads.isEmpty()) View.VISIBLE else View.GONE
                            }
                            is ThreadListUiState.Empty -> {
                                binding.tvEmptyState.text = state.message
                                binding.tvEmptyState.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
    }
}
