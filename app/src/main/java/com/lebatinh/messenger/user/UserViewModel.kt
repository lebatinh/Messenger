package com.lebatinh.messenger.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lebatinh.messenger.other.ReturnResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(private val userUseCase: UserUseCase) : ViewModel() {

    private val _returnResult = MutableLiveData<ReturnResult<User>?>()
    val returnResult: LiveData<ReturnResult<User>?> get() = _returnResult

    private val _unitResult = MutableLiveData<ReturnResult<Unit>?>()
    val unitResult: LiveData<ReturnResult<Unit>?> get() = _unitResult

    private val _selectedItems = MutableLiveData<MutableList<User>?>()
    val selectedItems: LiveData<MutableList<User>?> = _selectedItems

    private var currentSearchResult: Flow<PagingData<User>>? = null

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _returnResult.postValue(ReturnResult.Loading)
            val result = userUseCase.register(email, password)
            _returnResult.postValue(result)
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _returnResult.postValue(ReturnResult.Loading)
            val result = userUseCase.login(email, password)
            _returnResult.postValue(result)
        }
    }

    fun forgot(email: String, name: String) {
        viewModelScope.launch {
            _unitResult.postValue(ReturnResult.Loading)
            val result = userUseCase.forgotPass(email, name)
            _unitResult.postValue(result)
        }
    }

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            _unitResult.postValue(ReturnResult.Loading)
            val result = userUseCase.changePass(newPassword)
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
        viewModelScope.launch {
            _unitResult.postValue(ReturnResult.Loading)
            val result = userUseCase.updateUserInfo(email, fullName, phoneNumber, birthday, avatar)
            _unitResult.postValue(result)
        }
    }

    fun searchUsers(query: String, currentUserUID: String): Flow<PagingData<User>> {
        val newFlow =
            userUseCase.getSearchUsersPagingSource(query, currentUserUID).cachedIn(viewModelScope)
        currentSearchResult = newFlow
        return newFlow
    }

    fun getUserByUID(userUID: String) {
        viewModelScope.launch {
            _returnResult.postValue(ReturnResult.Loading)
            val result = userUseCase.getUserByUID(userUID)
            _returnResult.postValue(result)
        }
    }

    fun toggleSelection(user: User) {
        val currentList = _selectedItems.value ?: mutableListOf()
        if (currentList.contains(user)) {
            currentList.remove(user)
        } else {
            currentList.add(user)
        }
        _selectedItems.value = currentList
    }

    suspend fun getInfoUserByUID(userUID: String): User? {
        return userUseCase.getInfoUserByUID(userUID)
    }

    fun resetReturnResult() {
        _returnResult.postValue(null)
        _unitResult.postValue(null)
        _selectedItems.postValue(null)
    }
}