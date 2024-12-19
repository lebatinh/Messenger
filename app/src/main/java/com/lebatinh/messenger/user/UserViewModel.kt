package com.lebatinh.messenger.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.lebatinh.messenger.other.ReturnResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _returnResult = MutableLiveData<ReturnResult<User>?>()
    val returnResult: LiveData<ReturnResult<User>?> get() = _returnResult

    private val _unitResult = MutableLiveData<ReturnResult<Unit>?>()
    val unitResult: LiveData<ReturnResult<Unit>?> get() = _unitResult

    fun register(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _returnResult.postValue(ReturnResult.Error("Email hoặc mật khẩu không được để trống"))
            return
        }

        viewModelScope.launch {
            _returnResult.postValue(ReturnResult.Loading)
            try {
                val authResult = FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, password).await()
                val userUID = authResult.user?.uid
                if (userUID != null) {
                    val newUser = User(userUID = userUID, email = email)
                    val result = repository.register(newUser)
                    _returnResult.postValue(result)
                } else {
                    _returnResult.postValue(ReturnResult.Error("Có lỗi xảy ra! Hãy thử lại sau."))
                }
            } catch (e: Exception) {
                if (e is FirebaseAuthUserCollisionException) {
                    _returnResult.postValue(ReturnResult.Error("Email đã được đăng ký."))
                } else {
                    _returnResult.postValue(ReturnResult.Error("Có lỗi xảy ra! Hãy thử lại sau."))
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _returnResult.postValue(ReturnResult.Loading)
            val result = repository.login(email, password)
            _returnResult.postValue(result)
        }
    }

    fun forgot(email: String, name: String) {
        viewModelScope.launch {
            _returnResult.postValue(ReturnResult.Loading)
            val result = repository.checkIfUserExists(email, name)
            if (result is ReturnResult.Success && result.data) {
                _returnResult.postValue(ReturnResult.Success(User(email = email)))
            } else {
                _returnResult.postValue(
                    ReturnResult.Error("Thông tin tài khoản hoặc tên không chính xác.")
                )
            }
        }
    }

    fun changePassword(newPassword: String) {
        if (newPassword.isEmpty()) {
            _unitResult.postValue(ReturnResult.Error("Mật khẩu không được để trống."))
            return
        }

        viewModelScope.launch {
            _unitResult.postValue(ReturnResult.Loading)
            val result = repository.changePassword(newPassword)
            _unitResult.postValue(result)
        }
    }

    fun sendResetPasswordEmail(email: String) {
        if (email.isEmpty()) {
            _unitResult.postValue(ReturnResult.Error("Email không được để trống."))
            return
        }
        _unitResult.postValue(ReturnResult.Loading)

        viewModelScope.launch {
            val result = repository.sendResetPasswordEmail(email)
            _unitResult.postValue(result)
        }
    }

    fun updateUserInfo(
        email: String,
        fullName: String?,
        phoneNumber: String?,
        birthday: String?,
        avatar: String?
    ) {
        if (email.isEmpty()) {
            _unitResult.postValue(ReturnResult.Error("Có lỗi xảy ra!"))
            return
        }
        _unitResult.postValue(ReturnResult.Loading)

        viewModelScope.launch {
            val result = repository.updateUserInfo(email, fullName, phoneNumber, birthday, avatar)
            _unitResult.postValue(result)
        }
    }

    fun getUserInfo(email: String) {
        if (email.isEmpty()) {
            _returnResult.postValue(ReturnResult.Error("Có lỗi xảy ra!"))
            return
        }
        _returnResult.postValue(ReturnResult.Loading)

        viewModelScope.launch {
            val result = repository.getUserInfo(email)
            _returnResult.postValue(result)
        }
    }

    fun resetReturnResult() {
        _returnResult.postValue(null)
        _unitResult.postValue(null)
    }
}