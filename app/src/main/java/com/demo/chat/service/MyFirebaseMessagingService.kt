package com.demo.chat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.demo.chat.R
import com.demo.chat.ui.chat.ChatActivity

class MyFirebaseMessagingService {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "chat_notifications_channel"
        private const val CHANNEL_NAME = "Chat Notifications"

        fun showNotification(
            context: Context,
            title: String,
            message: String,
            chatId: String? = null
        ) {
            Log.d(TAG, "Triggering Notification: title=$title, body=$message")
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, ChatActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (chatId != null) {
                    putExtra(ChatActivity.EXTRA_THREAD_ID, chatId)
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_websocket)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}
