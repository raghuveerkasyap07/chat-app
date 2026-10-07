package com.demo.chat.ui.threads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.chat.DemoChatApplication
import com.demo.chat.data.remote.dto.UserDto
import com.demo.chat.databinding.DialogContactsBinding
import com.demo.chat.databinding.ItemContactBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class ContactsBottomSheetDialog(
    private val onContactSelected: (UserDto) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogContactsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvContacts.layoutManager = LinearLayoutManager(requireContext())

        loadContacts()
    }

    private fun loadContacts() {
        binding.pbContacts.visibility = View.VISIBLE
        binding.tvEmptyContacts.visibility = View.GONE

        val app = requireActivity().application as DemoChatApplication
        val userRepo = app.userRepository

        lifecycleScope.launch {
            val result = userRepo.getUsers()
            binding.pbContacts.visibility = View.GONE

            val contacts = result.getOrNull()
            if (!contacts.isNullOrEmpty()) {
                setupAdapter(contacts)
            } else {
                // Fallback to default demo contacts if server is unreachable
                val defaultContacts = listOf(
                    UserDto(id = "user_echo", name = "Echo Bot", email = "echo@bot.demo"),
                    UserDto(id = "user_alice", name = "Alice Smith", email = "alice@example.com"),
                    UserDto(id = "user_bob", name = "Bob Johnson", email = "bob@example.com"),
                    UserDto(id = "user_charlie", name = "Charlie Brown", email = "charlie@example.com"),
                    UserDto(id = "user_support", name = "Support Assistant", email = "support@demo.com")
                )
                setupAdapter(defaultContacts)
            }
        }
    }

    private fun setupAdapter(contacts: List<UserDto>) {
        if (contacts.isEmpty()) {
            binding.tvEmptyContacts.visibility = View.VISIBLE
            return
        }

        binding.rvContacts.adapter = ContactAdapter(contacts) { selectedUser ->
            dismiss()
            onContactSelected(selectedUser)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ContactAdapter(
        private val contacts: List<UserDto>,
        private val onClick: (UserDto) -> Unit
    ) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

        class ContactViewHolder(val binding: ItemContactBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
            val binding = ItemContactBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ContactViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
            val user = contacts[position]
            holder.binding.tvContactName.text = user.name
            holder.binding.tvContactStatus.text = user.email

            val initials = user.name.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
            holder.binding.tvContactInitials.text = if (initials.isNotEmpty()) initials else "CO"

            holder.binding.root.setOnClickListener {
                onClick(user)
            }
        }

        override fun getItemCount(): Int = contacts.size
    }
}
