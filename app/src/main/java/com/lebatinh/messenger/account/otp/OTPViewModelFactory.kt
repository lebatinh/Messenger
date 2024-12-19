package com.lebatinh.messenger.account.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class OTPViewModelFactory(private val otpRepository: OTPRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OTPViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OTPViewModel(otpRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}