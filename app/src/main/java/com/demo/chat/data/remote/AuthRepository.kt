package com.demo.chat.data.remote

import com.demo.chat.data.local.SessionManager
import com.demo.chat.data.remote.dto.AuthResponseDto
import com.demo.chat.data.remote.dto.UserDto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class AuthRepository(
    private val baseUrl: String = "http://10.0.2.2:5000",
    private val sessionManager: SessionManager,
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson()
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun register(name: String, email: String, password: String): Result<UserDto> =
        withContext(Dispatchers.IO) {
            try {
                val jsonBody = gson.toJson(
                    mapOf("name" to name, "email" to email, "password" to password)
                )
                val request = Request.Builder()
                    .url("$baseUrl/api/auth/register")
                    .post(jsonBody.toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val authResponse = gson.fromJson(responseBody, AuthResponseDto::class.java)
                    val data = authResponse.data
                    if (data != null) {
                        sessionManager.saveAuthToken(data.token)
                        sessionManager.saveUser(data.user.id, data.user.name, data.user.email)
                        Result.success(data.user)
                    } else {
                        Result.failure(Exception(authResponse.message ?: "Registration failed"))
                    }
                } else {
                    Result.failure(Exception("Registration failed (Code ${response.code})"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun login(email: String, password: String): Result<UserDto> =
        withContext(Dispatchers.IO) {
            try {
                val jsonBody = gson.toJson(
                    mapOf("email" to email, "password" to password)
                )
                val request = Request.Builder()
                    .url("$baseUrl/api/auth/login")
                    .post(jsonBody.toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val authResponse = gson.fromJson(responseBody, AuthResponseDto::class.java)
                    val data = authResponse.data
                    if (data != null) {
                        sessionManager.saveAuthToken(data.token)
                        sessionManager.saveUser(data.user.id, data.user.name, data.user.email)
                        Result.success(data.user)
                    } else {
                        Result.failure(Exception(authResponse.message ?: "Login failed"))
                    }
                } else {
                    Result.failure(Exception("Invalid credentials (Code ${response.code})"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getMe(): Result<UserDto> =
        withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getAuthToken()
                    ?: return@withContext Result.failure(Exception("No auth token"))

                val request = Request.Builder()
                    .url("$baseUrl/api/auth/me")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val user = gson.fromJson(responseBody, UserDto::class.java)
                    Result.success(user)
                } else {
                    Result.failure(Exception("Failed to fetch user profile"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
