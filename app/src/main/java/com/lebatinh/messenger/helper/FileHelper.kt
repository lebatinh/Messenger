package com.lebatinh.messenger.helper

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FileHelper {

    companion object {

        /**
         * Kích thước tối đa cho ảnh và video (bytes)
         */
        private const val MAX_IMAGE_SIZE = 5 * 1024 * 1024  // 5MB
        private const val MAX_VIDEO_SIZE = 20 * 1024 * 1024 // 20MB

        /**
         * Kiểm tra kích thước file có hợp lệ hay không
         */
        private fun isFileSizeValid(context: Context, uri: Uri, isImage: Boolean): Boolean {
            val fileSize = getFileSize(context, uri)
            return if (isImage) fileSize <= MAX_IMAGE_SIZE else fileSize <= MAX_VIDEO_SIZE
        }

        private fun getFileSize(context: Context, uri: Uri): Long {
            val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            val size = fileDescriptor?.statSize ?: 0
            fileDescriptor?.close()
            return size
        }

        /**
         * Upload file lên Cloudinary và trả về URL
         */
        private suspend fun uploadFile(uri: Uri, resourceType: String): String? {
            return withContext(Dispatchers.IO) {
                try {
                    suspendCancellableCoroutine<String> { continuation ->
                        MediaManager.get()
                            .upload(uri)
                            .option("resource_type", resourceType) // auto, image, hoặc video
                            .callback(object : UploadCallback {
                                override fun onStart(requestId: String?) {}

                                override fun onProgress(
                                    requestId: String?,
                                    bytes: Long,
                                    totalBytes: Long
                                ) {
                                }

                                override fun onSuccess(
                                    requestId: String?,
                                    resultData: MutableMap<Any?, Any?>?
                                ) {
                                    val secureUrl = resultData?.get("secure_url") as? String
                                    continuation.resume(secureUrl ?: "")
                                }

                                override fun onError(requestId: String?, error: ErrorInfo?) {
                                    continuation.resumeWithException(
                                        Throwable(error?.description ?: "Unknown error")
                                    )
                                }

                                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                                    continuation.resumeWithException(
                                        Throwable("Upload rescheduled: ${error?.description}")
                                    )
                                }

                            })
                            .dispatch()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }

        /**
         * Lưu ảnh và trả về URL
         */
        suspend fun uploadImage(context: Context, uri: Uri): String? {
            return if (isFileSizeValid(context, uri, isImage = true)) {
                uploadFile(uri, "image")
            } else {
                null
            }
        }

        /**
         * Lưu video và trả về URL
         */
        suspend fun uploadVideo(context: Context, uri: Uri): String? {
            return if (isFileSizeValid(context, uri, isImage = false)) {
                uploadFile(uri, "video")
            } else {
                null
            }
        }
    }
}