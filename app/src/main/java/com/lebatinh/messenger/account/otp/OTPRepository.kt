package com.lebatinh.messenger.account.otp

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.lebatinh.messenger.Key_Password.COLLECTION_PATH_OTP
import com.lebatinh.messenger.helper.GmailHelper
import com.lebatinh.messenger.other.ReturnResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

class OTPRepository {
    private val gmailHelper = GmailHelper()
    private val otpCollection = FirebaseFirestore.getInstance().collection(COLLECTION_PATH_OTP)

    suspend fun createAndSendOtp(email: String): ReturnResult<Unit> {
        val currentTime = System.currentTimeMillis()
        return try {
            val document = otpCollection.document(email).get().await()
            if (document.exists()) {
                val existingOtp = document.toObject(OTP::class.java)

                if (existingOtp != null) {
                    // Kiểm tra nếu OTP còn hiệu lực
                    if (currentTime <= existingOtp.timeOut) {
                        return ReturnResult.Error("OTP vẫn còn hiệu lực.")
                    }
                    // Nếu OTP đã hết hạn, xóa tài liệu cũ
                    otpCollection.document(email).delete().await()
                }
            }

            // Tạo OTP mới
            val newOtp = generateOtp()
            val otpData = OTP.createNew(newOtp)
            otpCollection.document(email).set(otpData).await()

            // Gửi OTP qua email
            val emailSent = sendEmailOTP(newOtp, email)
            if (emailSent) {
                ReturnResult.Success(Unit)
            } else {
                ReturnResult.Error("Yêu cầu OTP thất bại.")
            }
        } catch (e: Exception) {
            Log.e("error", e.toString())
            ReturnResult.Error("Lỗi không xác định! Hãy thử lại sau.")
        }
    }

    private suspend fun sendEmailOTP(otp: String, receiverEmail: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            gmailHelper.sendEmailOTP(otp, receiverEmail) { success, _ ->
                if (success) {
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }
        }
    }

    // Kiểm tra OTP
    suspend fun verifyOtp(email: String, inputOtp: String): ReturnResult<Unit> {
        return try {
            val document = otpCollection.document(email).get().await()
            if (document.exists()) {
                val existingOtp = document.toObject(OTP::class.java)
                if (existingOtp != null) {
                    if (System.currentTimeMillis() <= existingOtp.timeOut) {
                        if (existingOtp.otp == inputOtp) {
                            otpCollection.document(email).delete().await()
                            return ReturnResult.Success(Unit)
                        } else {
                            return ReturnResult.Error("OTP không chính xác.")
                        }
                    } else {
                        return ReturnResult.Error("OTP đã hết hạn.")
                    }
                }
            }
            ReturnResult.Error("OTP không tồn tại.")
        } catch (e: Exception) {
            Log.e("OTPRepository", "Error in verifyOtp", e)
            ReturnResult.Error("Lỗi không xác định! Hãy thử lại sau.")
        }
    }

    // Hàm sinh OTP ngẫu nhiên
    private fun generateOtp(): String {
        return (100000..999999).random().toString()
    }
}