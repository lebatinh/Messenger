package com.lebatinh.messenger.helper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.lebatinh.messenger.Key_Password.MAX_IMAGE_SIZE
import com.lebatinh.messenger.Key_Password.MAX_VIDEO_SIZE
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FileHelper {

    companion object {
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
         * Cắt ảnh
         */
        fun startCrop(
            activity: Activity,
            sourceUri: Uri,
            cropLauncher: ActivityResultLauncher<Intent>
        ) {
            val destinationUri = Uri.fromFile(File(activity.cacheDir, "cropped_image.jpg"))
            val options = UCrop.Options().apply {
                setCompressionFormat(Bitmap.CompressFormat.JPEG)
                setCompressionQuality(100)
                setFreeStyleCropEnabled(true)
            }
            val intent = UCrop.of(sourceUri, destinationUri)
                .withOptions(options)
                .getIntent(activity)

            cropLauncher.launch(intent)
        }

        /**
         * Xử lý ảnh cắt
         */
        fun handleCropResult(data: Intent?): Uri? {
            return data?.let { UCrop.getOutput(it) }
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