package com.lebatinh.messenger.account.otp

import com.google.firebase.firestore.FirebaseFirestore
import com.lebatinh.messenger.Key_Password.COLLECTION_PATH_OTP
import com.lebatinh.messenger.helper.GmailHelper
import com.lebatinh.messenger.other.ReturnResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class OTPRepository @Inject constructor(
    private val gmailHelper: GmailHelper,
    private val firestore: FirebaseFirestore
) {
    /**
     * tạo và gửi email otp cho :
     * @param email
     */
    suspend fun createOtp(email: String, otp: OTP): ReturnResult<Unit> {
        return try {
            firestore.collection(COLLECTION_PATH_OTP).document(email).set(otp).await()
            ReturnResult.Success(Unit)
        } catch (e: Exception) {
            ReturnResult.Error(e.message ?: "Lỗi không xác định")
        }
    }

    suspend fun sendEmailOTP(otp: String, email: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            gmailHelper.sendEmailOTP(otp, email) { success, _ ->
                continuation.resume(success)
            }
        }
    }

    /**
     * Kiểm tra OTP với
     * @param email
     */
    suspend fun getOtpByEmail(email: String): OTP? {
        return try {
            val document = firestore.collection(COLLECTION_PATH_OTP).document(email).get().await()
            if (document.exists()) {
                document.toObject(OTP::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteOtp(email: String) {
        firestore.collection(COLLECTION_PATH_OTP).document(email).delete().await()
    }
}