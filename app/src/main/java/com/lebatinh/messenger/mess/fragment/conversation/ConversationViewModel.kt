package com.lebatinh.messenger.mess.fragment.conversation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.lebatinh.messenger.notification.NotiData
import com.lebatinh.messenger.notification.NotiHelper
import com.lebatinh.messenger.other.MessageType
import com.lebatinh.messenger.other.NotificationType
import com.lebatinh.messenger.other.ReturnResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val useCase: ConversationUseCase,
    private val notiHelper: NotiHelper
) : ViewModel() {
    private val _conversationResult = MutableLiveData<ReturnResult<Conversation>?>()
    val conversationResult: LiveData<ReturnResult<Conversation>?> get() = _conversationResult

    private var currentConversationsFlow: Flow<PagingData<Conversation>>? = null

    private val _unitResult = MutableLiveData<ReturnResult<Unit>?>()
    val unitResult: LiveData<ReturnResult<Unit>?> get() = _unitResult

    private val _listMessageResult = MutableLiveData<ReturnResult<List<Message>>?>()
    val listMessageResult: LiveData<ReturnResult<List<Message>>?> get() = _listMessageResult
    private var messageJob: Job? = null

    private val _messageResult = MutableLiveData<ReturnResult<Conversation>?>()
    val messageResult: LiveData<ReturnResult<Conversation>?> get() = _messageResult

    private val _conversationId = MutableLiveData<String?>()
    val conversationId: LiveData<String?> get() = _conversationId

    fun setConversationId(newId: String?) {
        if (_conversationId.value != newId) {
            _conversationId.value = newId
        }
    }

    fun createConversation(
        isGroup: Boolean,
        groupName: String,
        groupImage: String? = null,
        memberIds: List<String>
    ) {
        viewModelScope.launch {
            _conversationResult.postValue(ReturnResult.Loading)
            val result = useCase.createConversation(isGroup, groupName, groupImage, memberIds)
            _conversationResult.postValue(result)
        }
    }

    fun getConversationByGroupId(groupId: String) {
        viewModelScope.launch {
            _conversationResult.postValue(ReturnResult.Loading)
            val result = useCase.getConversationByGroupId(groupId)
            _conversationResult.postValue(result)
        }
    }

    fun updateConversationToGroup(
        conversationId: String,
        newUserIds: List<String>
    ) {
        viewModelScope.launch {
            _unitResult.postValue(ReturnResult.Loading)
            val result = useCase.updateConversationToGroup(conversationId, newUserIds)
            _unitResult.postValue(result)
        }
    }

    fun getConversationsByUserId(
        currentUID: String,
        isGroup: Boolean? = null
    ): Flow<PagingData<Conversation>> {
        val lastResult = currentConversationsFlow
        if (lastResult != null) {
            return lastResult
        }

        val newResult = useCase.getConversationsPagingFlow(currentUID, isGroup)
            .cachedIn(viewModelScope)
        currentConversationsFlow = newResult
        return newResult
    }

    fun sendMessage(
        senderId: String,
        receiverId: String?,
        isGroup: Boolean,
        conversationId: String?,
        type: MessageType,
        message: String?,
        urlMedia: List<String>?
    ) {
        viewModelScope.launch {
            val result = useCase.sendMessage(
                senderId,
                receiverId,
                isGroup,
                conversationId,
                type,
                message,
                urlMedia
            )
            _messageResult.postValue(result)
        }
    }

    fun getConversationIdForOneToOne(
        currentUID: String,
        receiverUID: String
    ) {
        viewModelScope.launch {
            val result = useCase.getConversationIdForOneToOne(currentUID, receiverUID)
            _conversationId.postValue(result)
        }
    }

    fun getMessages(conversationId: String) {
        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            useCase.getMessages(conversationId)
                .onStart {
                    _listMessageResult.postValue(ReturnResult.Loading)
                }
                .catch { e ->
                    _listMessageResult.postValue(
                        ReturnResult.Error(
                            e.message ?: "Unexpected error"
                        )
                    )
                }
                .collect { result ->
                    _listMessageResult.postValue(result)
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        messageJob?.cancel()
    }

    fun sendMessageNotification(
        senderId: String,
        type: NotificationType,
        receiverId: String,
        message: String,
        senderName: String,
        conversationId: String
    ) {
        viewModelScope.launch {
            try {
                // Thêm data để xử lý khi user click vào notification
                val data = mapOf(
                    "type" to type,
                    "sender_id" to senderId,
                    "sender_name" to senderName,
                    "conversation_id" to conversationId,
                    "timestamp" to System.currentTimeMillis().toString(),
                    "action" to "OPEN_CHAT" // Để biết phải mở màn hình nào khi click
                )

                val notification = NotiData(
                    title = senderName,
                    message = message,
                    data = data
                )

                val success = notiHelper.sendNotificationToTopic(
                    topic = receiverId,
                    notification = notification
                )

                if (!success) {
                    Log.e("ChatViewModel", "Failed to send message notification to: $receiverId")
                } else {
                    Log.d("ChatViewModel", "Successfully sent message notification to: $receiverId")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message notification", e)
            }
        }
    }

    fun resetResult() {
        _conversationResult.postValue(null)
        _unitResult.postValue(null)
        _listMessageResult.postValue(null)
    }
}