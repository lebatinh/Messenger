package com.lebatinh.messenger

import android.app.Application
import com.cloudinary.android.MediaManager
import com.lebatinh.messenger.Key_Password.API_KEY
import com.lebatinh.messenger.Key_Password.API_SECRET
import com.lebatinh.messenger.Key_Password.CLOUD_NAME
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = mapOf(
            "cloud_name" to CLOUD_NAME,
            "api_key" to API_KEY,
            "api_secret" to API_SECRET
        )
        MediaManager.init(this, config)

        EmojiManager.install(GoogleEmojiProvider())
    }
}