package com.demo.chat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.demo.chat.R
import com.demo.chat.ui.chat.ChatActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.HttpURLConnection
import java.net.URL

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFCMService"
        private const val CHANNEL_ID = "chat_notifications_channel"
        private const val NOTIFICATION_ID = 1002
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM Token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "New Chat Message"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "You received a new message or photo attachment"
        val imageUrl = remoteMessage.data["imageUrl"] ?: remoteMessage.notification?.imageUrl?.toString()
        val threadId = remoteMessage.data["threadId"] ?: "thread_echo_bot"
        val peerName = remoteMessage.data["peerName"] ?: "Echo Bot"

        showNotification(title, body, imageUrl, threadId, peerName)
    }

    private fun showNotification(title: String, body: String, imageUrl: String?, threadId: String, peerName: String) {
        createNotificationChannel()

        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra(ChatActivity.EXTRA_THREAD_ID, threadId)
            putExtra(ChatActivity.EXTRA_PEER_NAME, peerName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            threadId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_websocket)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        if (!imageUrl.isNullOrBlank()) {
            val bitmap = getBitmapFromUrl(imageUrl)
            if (bitmap != null) {
                builder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null as Bitmap?)
                )
                    .setLargeIcon(bitmap)
            }
        }

        try {
            with(NotificationManagerCompat.from(this)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to post notification due to missing permission", e)
        }
    }

    private fun getBitmapFromUrl(urlString: String): Bitmap? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download notification image", e)
            null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Chat Notifications"
            val descriptionText = "Notifications for incoming chat messages and photo attachments"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
