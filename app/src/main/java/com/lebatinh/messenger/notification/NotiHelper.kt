package com.lebatinh.messenger.notification

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.messaging.FirebaseMessaging
import com.lebatinh.messenger.Key_Password.YOUR_PROJECT_ID
import com.lebatinh.messenger.R
import com.lebatinh.messenger.notification.NotificationChannelConfig.DEFAULT_CHANNEL_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NotiHelper(private val context: Context) {
    companion object {
        private const val FCM_API_URL =
            "https://fcm.googleapis.com/v1/projects/$YOUR_PROJECT_ID/messages:send"
        private const val RETRY_COUNT = 3
        private const val RETRY_DELAY = 2000L // 2 second
    }

    private var accessToken: String? = null
    private var tokenExpiration: Long = 0

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private suspend fun getAccessToken(): String {
        return withContext(Dispatchers.IO) {
            try {
                if (accessToken != null && System.currentTimeMillis() < tokenExpiration) {
                    return@withContext accessToken!!
                }

                val credentials = context.resources.openRawResource(R.raw.service_account).use {
                    GoogleCredentials
                        .fromStream(it)
                        .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
                }
                credentials.refreshIfExpired()
                accessToken = credentials.accessToken.tokenValue
                tokenExpiration = System.currentTimeMillis() +
                        (credentials.accessToken.expirationTime.time - System.currentTimeMillis()) * 9 / 10

                accessToken!!
            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun subscribeToTopic(topic: String): Boolean {
        return try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun unsubscribeFromTopic(topic: String): Boolean {
        return try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendNotificationToTopic(
        topic: String,
        notification: NotiData,
        retryCount: Int = RETRY_COUNT
    ): Boolean = withContext(Dispatchers.IO) {
        repeat(retryCount) { attempt ->
            try {
                val accessToken = getAccessToken()

                val message = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("topic", topic)

                        put("notification", JSONObject().apply {
                            put("title", notification.title)
                            put("body", notification.message)
                        })

                        if (notification.data.isNotEmpty()) {
                            put("data", JSONObject(notification.data))
                        }

                        put("android", JSONObject().apply {
                            put("notification", JSONObject().apply {
                                put("channel_id", DEFAULT_CHANNEL_ID)
                                put("notification_priority", "PRIORITY_HIGH")
                                put("sound", "default")
                            })
                        })
                    })
                }

                // Tạo request body
                val requestBody = message.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                // Tạo request
                val request = Request.Builder()
                    .url(FCM_API_URL)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(requestBody)
                    .build()

                // Thực hiện request
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        return@withContext true
                    } else {
                        val errorBody = response.body?.string()
                        throw Exception(
                            "Failed to send notification. Response code: ${response.code}, " +
                                    "Error: $errorBody"
                        )
                    }
                }
            } catch (e: Exception) {
                if (attempt < retryCount - 1) {
                    delay(RETRY_DELAY * (attempt + 1))
                }
            }
        }
        false
    }
}