package com.demo.chat.data.remote

import com.demo.chat.data.local.SessionManager
import com.demo.chat.data.remote.dto.ChatDto
import com.demo.chat.data.remote.dto.ChatListResponseDto
import com.demo.chat.data.remote.dto.ChatResponseDto
import com.demo.chat.data.remote.dto.MessageDataDto
import com.demo.chat.data.remote.dto.MessageListResponseDto
import com.demo.chat.data.remote.dto.SendMessageResponseDto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ChatRepository(
    private val baseUrl: String = "http://10.0.2.2:5000",
    private val sessionManager: SessionManager,
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson()
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getChats(): Result<List<ChatDto>> =
        withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAuthToken()
                    ?: return@withContext Result.failure(Exception("Not authenticated"))

                val request = Request.Builder()
                    .url("$baseUrl/api/chats")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val listResponse = gson.fromJson(responseBody, ChatListResponseDto::class.java)
                    Result.success(listResponse.data)
                } else {
                    Result.failure(Exception("Failed to fetch chats"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createOrGetChat(recipientId: String): Result<ChatDto> =
        withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAuthToken()
                    ?: return@withContext Result.failure(Exception("Not authenticated"))

                val jsonBody = gson.toJson(mapOf("recipientId" to recipientId))
                val request = Request.Builder()
                    .url("$baseUrl/api/chats")
                    .addHeader("Authorization", "Bearer $token")
                    .post(jsonBody.toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val chatResponse = gson.fromJson(responseBody, ChatResponseDto::class.java)
                    val chat = chatResponse.data?.chat
                    if (chat != null) {
                        Result.success(chat)
                    } else {
                        Result.failure(Exception("Chat creation failed"))
                    }
                } else {
                    Result.failure(Exception("Failed to create chat (Code ${response.code})"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getChatMessages(chatId: String, page: Int = 1, limit: Int = 50): Result<List<MessageDataDto>> =
        withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAuthToken()
                    ?: return@withContext Result.failure(Exception("Not authenticated"))

                val request = Request.Builder()
                    .url("$baseUrl/api/chats/$chatId/messages?page=$page&limit=$limit")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val messageList = gson.fromJson(responseBody, MessageListResponseDto::class.java)
                    Result.success(messageList.data)
                } else {
                    Result.failure(Exception("Failed to load messages"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun sendMessageRest(chatId: String, text: String): Result<MessageDataDto> =
        withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAuthToken()
                    ?: return@withContext Result.failure(Exception("Not authenticated"))

                val jsonBody = gson.toJson(mapOf("message" to text))
                val request = Request.Builder()
                    .url("$baseUrl/api/chats/$chatId/messages")
                    .addHeader("Authorization", "Bearer $token")
                    .post(jsonBody.toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val sendResponse = gson.fromJson(responseBody, SendMessageResponseDto::class.java)
                    val messageData = sendResponse.data
                    if (messageData != null) {
                        Result.success(messageData)
                    } else {
                        Result.failure(Exception("Failed to send message"))
                    }
                } else {
                    Result.failure(Exception("Failed to send message (Code ${response.code})"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
