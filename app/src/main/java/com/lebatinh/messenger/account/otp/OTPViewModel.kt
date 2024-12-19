package com.lebatinh.messenger.account.otp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lebatinh.messenger.other.ReturnResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OTPViewModel @Inject constructor(private val otpUseCase: OTPUseCase) : ViewModel() {
    private val _otpResult = MutableLiveData<ReturnResult<Unit>?>()
    val otpResult: LiveData<ReturnResult<Unit>?> get() = _otpResult

    fun createAndSendOtp(email: String) {
        viewModelScope.launch {
            _otpResult.value = ReturnResult.Loading
            val result = otpUseCase.createAndSendOtp(email)
            _otpResult.postValue(result)
        }
    }

    fun verifyOtp(email: String, inputOtp: String) {
        viewModelScope.launch {
            _otpResult.postValue(ReturnResult.Loading)
            val result = otpUseCase.verifyOTP(email, inputOtp)
            _otpResult.postValue(result)
        }
    }

    fun resetReturnResult() {
        _otpResult.postValue(null)
    }
}