package com.demo.chat.data.remote

import com.demo.chat.data.local.SessionManager
import com.demo.chat.data.remote.dto.SendMessageResponseDto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class MediaRepository(
    private val baseUrl: String = "http://10.0.2.2:5000",
    private val sessionManager: SessionManager,
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson()
) {

    private val imageMediaType = "image/jpeg".toMediaType()

    suspend fun uploadPhotoAttachment(chatId: String, photoFile: File): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAuthToken()
                    ?: return@withContext Result.failure(Exception("Not authenticated"))

                val filePart = photoFile.asRequestBody(imageMediaType)
                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("attachment", photoFile.name, filePart)
                    .build()

                val request = Request.Builder()
                    .url("$baseUrl/api/chats/$chatId/attachments")
                    .addHeader("Authorization", "Bearer $token")
                    .post(multipartBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val uploadResponse = gson.fromJson(responseBody, SendMessageResponseDto::class.java)
                    val mediaUrl = uploadResponse.data?.mediaUrl
                    if (!mediaUrl.isNullOrBlank()) {
                        Result.success(mediaUrl)
                    } else {
                        Result.failure(Exception("No media URL returned"))
                    }
                } else {
                    Result.failure(Exception("Upload failed (Code ${response.code})"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
