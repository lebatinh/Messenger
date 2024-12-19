package com.lebatinh.messenger.account.otp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lebatinh.messenger.other.ReturnResult
import kotlinx.coroutines.launch

class OTPViewModel(private val otpRepository: OTPRepository) : ViewModel() {
    private val _otpResult = MutableLiveData<ReturnResult<Unit>?>()
    val otpResult: LiveData<ReturnResult<Unit>?> get() = _otpResult

    private val _otpVerificationResult = MutableLiveData<ReturnResult<Unit>?>()
    val otpVerificationResult: LiveData<ReturnResult<Unit>?> get() = _otpVerificationResult

    fun createAndSendOtp(email: String) {
        viewModelScope.launch {
            _otpResult.value = ReturnResult.Loading
            val result = otpRepository.createAndSendOtp(email)
            _otpResult.postValue(result)
        }
    }

    fun verifyOtp(email: String, inputOtp: String) {
        viewModelScope.launch {
            _otpVerificationResult.postValue(ReturnResult.Loading)
            val result = otpRepository.verifyOtp(email, inputOtp)
            _otpVerificationResult.postValue(result)
        }
    }

    fun resetReturnResult() {
        _otpResult.postValue(null)
    }

    fun resetOtpVerificationResult() {
        _otpVerificationResult.postValue(null)
    }
}