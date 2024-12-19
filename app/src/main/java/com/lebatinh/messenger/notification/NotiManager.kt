package com.lebatinh.messenger.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.lebatinh.messenger.R
import com.lebatinh.messenger.mess.fragment.community.FriendFragment
import com.lebatinh.messenger.mess.fragment.conversation.ConversationFragment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotiManager @Inject constructor(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        // Kênh mặc định
        createChannel(
            NotificationChannelConfig.DEFAULT_CHANNEL_ID,
            NotificationChannelConfig.DEFAULT_CHANNEL_NAME,
            IMPORTANCE_HIGH
        )

        // Kênh tin nhắn
        createChannel(
            NotificationChannelConfig.MESSAGE_CHANNEL_ID,
            NotificationChannelConfig.MESSAGE_CHANNEL_NAME,
            IMPORTANCE_HIGH
        )
    }

    private fun createChannel(
        channelId: String,
        channelName: String,
        importance: Int
    ): NotificationChannel {
        return NotificationChannel(channelId, channelName, importance).apply {
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
            notificationManager.createNotificationChannel(this)
        }
    }

    fun showNotification(
        title: String,
        message: String,
        data: Map<String, String> = emptyMap(),
        channelId: String = NotificationChannelConfig.DEFAULT_CHANNEL_ID
    ) {
        try {
            // Tạo intent để mở app khi click vào notification
            val intent =
                context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    data.forEach { (key, value) ->
                        putExtra(key, value)
                    }
                }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Build notification
            val builder = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .apply {
                    // Thêm actions tùy theo loại thông báo
                    when (data["type"]) {
                        "message" -> {
                            // Add quick reply action
                            addAction(
                                R.drawable.reply,
                                "Trả lời",
                                createReplyPendingIntent(data)
                            )
                        }

                        "friend_request" -> {
                            // Add accept/decline actions
                            addAction(
                                R.drawable.invitation,
                                "Chấp nhận",
                                createAcceptPendingIntent(data)
                            )
                            addAction(
                                R.drawable.decline_invitation,
                                "Từ chối",
                                createDeclinePendingIntent(data)
                            )
                        }
                    }
                }

            // Show notification
            notificationManager.notify(
                System.currentTimeMillis().toInt(),
                builder.build()
            )
        } catch (_: Exception) {
        }
    }

    private fun createReplyPendingIntent(data: Map<String, String>): PendingIntent {
        val replyIntent = Intent(context, ConversationFragment::class.java).apply {
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            replyIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createAcceptPendingIntent(data: Map<String, String>): PendingIntent {
        val acceptIntent = Intent(context, FriendFragment::class.java).apply {
            action = "ACCEPT"
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            acceptIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createDeclinePendingIntent(data: Map<String, String>): PendingIntent {
        val declineIntent = Intent(context, FriendFragment::class.java).apply {
            action = "DECLINE"
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            declineIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}