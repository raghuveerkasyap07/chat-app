package com.demo.chat.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    companion object {
        private const val PREF_NAME = "demo_chat_session"

        // Primary Running Server IP (Text Chat Backend API):
        const val HOST_SERVER = "192.168.0.10:5000"

        const val DEFAULT_BASE_URL = "http://$HOST_SERVER"
        const val DEFAULT_WS_URL = "ws://$HOST_SERVER"
        const val PUBLIC_ECHO_URL = "wss://ws.postman-echo.com/raw"

        private const val KEY_BASE_URL = "key_base_url"
        private const val KEY_WS_URL = "key_ws_url"
        private const val KEY_AUTH_TOKEN = "key_auth_token"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_EMAIL = "key_user_email"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getBaseUrl(): String {
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun getWsUrl(): String {
        return prefs.getString(KEY_WS_URL, DEFAULT_WS_URL) ?: DEFAULT_WS_URL
    }

    fun saveServerConfig(baseUrl: String, wsUrl: String) {
        prefs.edit()
            .putString(KEY_BASE_URL, baseUrl)
            .putString(KEY_WS_URL, wsUrl)
            .apply()
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun saveUser(userId: String, userName: String, email: String) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, userName)
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    fun isLoggedIn(): Boolean {
        return !getAuthToken().isNullOrBlank()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
