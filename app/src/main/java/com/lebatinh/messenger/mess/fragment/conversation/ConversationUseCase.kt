package com.lebatinh.messenger.mess.fragment.conversation

import com.lebatinh.messenger.other.MessageType
import com.lebatinh.messenger.other.ReturnResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ConversationUseCase @Inject constructor(
    private val repository: ConversationRepository
) {
    suspend fun createConversation(
        isGroup: Boolean,
        groupName: String,
        groupImage: String? = null,
        memberIds: List<String>
    ): ReturnResult<Conversation> {
        return repository.createConversation(isGroup, groupName, groupImage, memberIds)
    }

    suspend fun getConversationByGroupId(groupId: String): ReturnResult<Conversation> {
        return repository.getConversationByGroupId(groupId)
    }

    suspend fun updateConversationToGroup(
        conversationId: String,
        newUserIds: List<String>
    ): ReturnResult<Unit> {
        return repository.updateConversationToGroup(conversationId, newUserIds)
    }

    suspend fun getConversationsByUserId(
        currentUID: String,
        isGroup: Boolean? = null
    ): ReturnResult<List<Conversation>> {
        return repository.getConversationsByUserId(currentUID, isGroup)
    }

    suspend fun sendMessage(
        senderId: String,
        receiverId: String?,
        isGroup: Boolean,
        conversationId: String?,
        type: MessageType,
        message: String?,
        urlMedia: List<String>?
    ): ReturnResult<Conversation> {
        return repository.sendMessage(
            senderId,
            receiverId,
            isGroup,
            conversationId,
            type,
            message,
            urlMedia
        )
    }

    suspend fun getConversationIdForOneToOne(
        currentUID: String,
        receiverUID: String
    ): String? {
        return repository.getConversationIdForOneToOne(currentUID, receiverUID)
    }

    fun getMessages(conversationId: String): Flow<ReturnResult<List<Message>>> {
        return repository.getMessages(conversationId)
            .map { result ->
                when (result) {
                    is ReturnResult.Success -> {
                        ReturnResult.Success(result.data)
                    }

                    else -> result
                }
            }.catch { e ->
                emit(ReturnResult.Error(e.message ?: "Lỗi tải tin nhắn"))
            }
    }
}