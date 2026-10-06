package com.demo.chat.data.remote

import com.demo.chat.data.local.SessionManager
import com.demo.chat.data.remote.dto.UserDto
import com.demo.chat.data.remote.dto.UsersListResponseDto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class UserRepository(
    private val baseUrl: String = "http://10.0.2.2:5000",
    private val sessionManager: SessionManager,
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson()
) {

    suspend fun getUsers(search: String? = null): Result<List<UserDto>> =
        withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAuthToken()
                    ?: return@withContext Result.failure(Exception("Not authenticated"))

                val query = if (!search.isNullOrBlank()) "?search=$search" else ""
                val request = Request.Builder()
                    .url("$baseUrl/api/users$query")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val listResponse = gson.fromJson(responseBody, UsersListResponseDto::class.java)
                    Result.success(listResponse.data)
                } else {
                    Result.failure(Exception("Failed to fetch users"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getUserById(userId: String): Result<UserDto> =
        withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAuthToken()
                    ?: return@withContext Result.failure(Exception("Not authenticated"))

                val request = Request.Builder()
                    .url("$baseUrl/api/users/$userId")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val user = gson.fromJson(responseBody, UserDto::class.java)
                    Result.success(user)
                } else {
                    Result.failure(Exception("User not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
