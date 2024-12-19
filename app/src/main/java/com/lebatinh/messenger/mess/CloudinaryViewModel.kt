package com.lebatinh.messenger.mess

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudinaryViewModel @Inject constructor(
    private val repository: CloudinaryRepository
) : ViewModel() {
    private val _imageUrl = MutableLiveData<List<String>>()
    val imageUrl: LiveData<List<String>> = _imageUrl

    private val _videoUrl = MutableLiveData<List<String>>()
    val videoUrl: LiveData<List<String>> = _videoUrl

    fun uploadImage(uris: List<Uri>, context: Context) {
        viewModelScope.launch {
            val listImage = mutableListOf<String>()
            uris.forEach { uri ->
                val url = repository.uploadImage(context, uri)
                url?.let { listImage.add(it) }
            }
            _imageUrl.postValue(listImage)
        }
    }

    fun uploadVideo(uris: List<Uri>, context: Context) {
        viewModelScope.launch {
            val listVideo = mutableListOf<String>()
            uris.forEach { uri ->
                val url = repository.uploadVideo(context, uri)
                url?.let { listVideo.add(it) }
            }
            _videoUrl.postValue(listVideo)
        }
    }
}