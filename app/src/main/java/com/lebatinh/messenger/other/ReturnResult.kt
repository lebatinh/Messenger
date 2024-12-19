package com.lebatinh.messenger.other

sealed class ReturnResult<out T> {
    object Loading : ReturnResult<Nothing>()
    data class Success<T>(val data: T) : ReturnResult<T>()
    data class Error(val message: String) : ReturnResult<Nothing>()
}