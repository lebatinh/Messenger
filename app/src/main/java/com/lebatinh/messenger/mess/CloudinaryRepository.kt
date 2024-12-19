package com.lebatinh.messenger.mess

import android.content.Context
import android.net.Uri
import com.lebatinh.messenger.helper.FileHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudinaryRepository @Inject constructor() {

    suspend fun uploadImage(context: Context, uri: Uri): String? {
        return FileHelper.uploadImage(context, uri)
    }

    suspend fun uploadVideo(context: Context, uri: Uri): String? {
        return FileHelper.uploadVideo(context, uri)
    }
}