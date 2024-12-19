package com.lebatinh.messenger.account.otp

import com.lebatinh.messenger.Key_Password.DEFAULT_TIMEOUT

data class OTP(
    val otp: String = "",
    val timeCreate: Long = 0L,
    val timeOut: Long = 0L
) {
    companion object {

        private fun getCurrentTime(): Long {
            return System.currentTimeMillis()
        }

        fun createNew(otp: String): OTP {
            val currentTime = getCurrentTime()
            return OTP(
                otp = otp,
                timeCreate = currentTime,
                timeOut = currentTime + DEFAULT_TIMEOUT
            )
        }
    }
}
