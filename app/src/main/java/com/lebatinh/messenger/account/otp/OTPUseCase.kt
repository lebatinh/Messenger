package com.lebatinh.messenger.account.otp

import com.lebatinh.messenger.other.ReturnResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OTPUseCase @Inject constructor(
    private val repository: OTPRepository
) {
    suspend fun createAndSendOtp(email: String): ReturnResult<Unit> {
        val otp = generateOtp()
        val otpData = OTP.createNew(otp)

        val dbResult = repository.createOtp(email, otpData)
        if (dbResult is ReturnResult.Success) {
            val emailResult = repository.sendEmailOTP(otp, email)
            return if (emailResult) {
                ReturnResult.Success(Unit)
            } else {
                ReturnResult.Error("Gửi email thất bại")
            }
        }
        return dbResult
    }

    private fun generateOtp(): String {
        return (100000..999999).random().toString()
    }

    suspend fun verifyOTP(email: String, inputOtp: String): ReturnResult<Unit> {
        val otp = repository.getOtpByEmail(email) ?: return ReturnResult.Error("OTP không tồn tại.")

        return if (System.currentTimeMillis() <= otp.timeOut) {
            if (otp.otp == inputOtp) {
                repository.deleteOtp(email)
                ReturnResult.Success(Unit)
            } else {
                ReturnResult.Error("OTP không chính xác.")
            }
        } else {
            ReturnResult.Error("OTP đã hết hạn.")
        }
    }
}