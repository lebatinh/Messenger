package com.lebatinh.messenger.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notiManager: NotiManager

    override fun onCreate() {
        super.onCreate()
        if (!::notiManager.isInitialized) {
            notiManager = NotiManager(applicationContext)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Xử lý thông báo khi app đang chạy
        remoteMessage.notification?.let { notification ->
            val channelId = when (remoteMessage.data["type"]) {
                "message" -> NotificationChannelConfig.MESSAGE_CHANNEL_ID
                else -> NotificationChannelConfig.DEFAULT_CHANNEL_ID
            }

            notiManager.showNotification(
                title = notification.title ?: "",
                message = notification.body ?: "",
                data = remoteMessage.data,
                channelId = channelId
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Gửi token mới lên server nếu cần
        Log.d("FCM", "New token: $token")
    }
}