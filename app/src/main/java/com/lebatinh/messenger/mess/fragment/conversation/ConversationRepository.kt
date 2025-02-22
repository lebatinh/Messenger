package com.lebatinh.messenger.mess.fragment.conversation

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.lebatinh.messenger.Key_Password.COLLECTION_PATH_CONVERSATION
import com.lebatinh.messenger.Key_Password.COLLECTION_PATH_MESSAGE
import com.lebatinh.messenger.Key_Password.PAGE_SIZE
import com.lebatinh.messenger.other.MessageType
import com.lebatinh.messenger.other.ReturnResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val realtimeDB: FirebaseDatabase
) {
    /**
     * Tạo nhóm chat với:
     * @param name
     * @param groupImage
     * @param memberIds
     */
    suspend fun createConversation(
        isGroup: Boolean,
        name: String? = null,
        groupImage: String? = null,
        memberIds: List<String>
    ): ReturnResult<Conversation> {
        return try {
            val conversationId = firestore.collection(COLLECTION_PATH_CONVERSATION).document().id

            val conversation = Conversation(conversationId, isGroup, groupImage, memberIds, name)

            firestore.collection(COLLECTION_PATH_CONVERSATION)
                .document(conversationId)
                .set(conversation)
                .await()

            ReturnResult.Success(conversation)
        } catch (e: Exception) {
            ReturnResult.Error(e.localizedMessage ?: "Lỗi khi tạo group")
        }
    }

    /**
     * Lấy nhóm chat với:
     * @param groupId
     */
    suspend fun getConversationByGroupId(groupId: String): ReturnResult<Conversation> {
        return try {
            val snapshot = firestore.collection(COLLECTION_PATH_CONVERSATION)
                .document(groupId)
                .get()
                .await()

            val conversation = snapshot.toObject(Conversation::class.java)
            if (conversation != null) {
                ReturnResult.Success(conversation)
            } else {
                ReturnResult.Error("Không tìm thấy thông tin nhóm")
            }
        } catch (e: Exception) {
            ReturnResult.Error(e.localizedMessage ?: "Lỗi khi lấy thông tin nhóm")
        }
    }

    /**
     * Cập nhật cuộc trò chuyện 1-1 thành group với:
     * @param conversationId : id cuộc trò chuyện
     * @param newUserIds : danh sách người mới được thêm vào
     */
    suspend fun updateConversationToGroup(
        conversationId: String,
        newUserIds: List<String>
    ): ReturnResult<Unit> {
        return try {
            val conversationRef =
                firestore.collection(COLLECTION_PATH_CONVERSATION).document(conversationId)
            val snapshot = conversationRef.get().await()

            if (snapshot.exists()) {
                val currentList = snapshot.get("listIdChatPerson") as? List<*>
                val updatedList =
                    currentList?.toMutableSet()?.apply { addAll(newUserIds) }?.toList()

                conversationRef.update(
                    mapOf(
                        "isGroup" to true,
                        "listIdChatPerson" to updatedList
                    )
                ).await()

                ReturnResult.Success(Unit)
            } else {
                ReturnResult.Error("Cuộc hội thoại không tồn tại.")
            }
        } catch (e: Exception) {
            ReturnResult.Error(e.localizedMessage ?: "Lỗi khi cập nhật cuộc hội thoại thành nhóm")
        }
    }

    /**
     * Lấy toàn bộ các cuộc hội thoại với:
     * @param currentUID: ID của người dùng hiện tại
     * @param isGroup: Nếu true, chỉ lấy các nhóm;
     * nếu false, chỉ lấy các cuộc trò chuyện cá nhân; nếu null, lấy tất cả
     */
    private var currentPagingSource: ConversationPagingSource? = null

    fun getConversationsPagingFlow(
        currentUID: String,
        isGroup: Boolean?
    ): Flow<PagingData<Conversation>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE
            ),
            pagingSourceFactory = {
                ConversationPagingSource(firestore, currentUID, isGroup).also {
                    currentPagingSource = it
                }
            }
        ).flow
    }
    // Add a method to listen for real-time updates outside of paging
    fun getRealtimeConversations(
        currentUID: String,
        isGroup: Boolean? = null,
        onUpdate: (List<Conversation>) -> Unit
    ): ListenerRegistration {
        val query = firestore.collection(COLLECTION_PATH_CONVERSATION)
            .whereArrayContains("listIdChatPerson", currentUID)

        val filteredQuery = when (isGroup) {
            true -> query.whereEqualTo("group", true)
            false -> query.whereEqualTo("group", false)
            else -> query
        }

        return filteredQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            snapshot?.let { querySnapshot ->
                val conversations = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Conversation::class.java)
                }
                onUpdate(conversations)
            }
        }
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
        val messagesRef = realtimeDB.getReference(COLLECTION_PATH_MESSAGE)
        val conversationsRef = firestore.collection(COLLECTION_PATH_CONVERSATION)

        // Khởi tạo các biến cần thiết
        val timestamp = System.currentTimeMillis()
        var finalConversationId = conversationId

        try {
            if (!isGroup) {
                // Kiểm tra xem đã có conversationId cho 1-1 chưa
                finalConversationId = getConversationIdForOneToOne(senderId, receiverId ?: "")

                if (finalConversationId == null) {
                    // Tạo mới conversationId
                    finalConversationId = messagesRef.push().key
                        ?: return ReturnResult.Error("Lỗi tạo cuộc trò chuyện!")

                    // Lưu conversation mới vào Firestore
                    val conversationData = Conversation(
                        finalConversationId,
                        false,
                        null,
                        listOf(senderId, receiverId!!),
                        null,
                        null
                    )
                    conversationsRef.document(finalConversationId).set(conversationData).await()
                }
            }

            if (finalConversationId.isNullOrEmpty()) {
                return ReturnResult.Error("Cuộc trò chuyện không hợp lệ!")
            }

            // Tạo messageId và dữ liệu tin nhắn
            val messageId = messagesRef.child(finalConversationId).push().key
                ?: return ReturnResult.Error("Lỗi tạo tin nhắn!")

            val messageData = Message(
                id = messageId,
                senderId = senderId,
                type = type,
                message = message,
                urlMedia = urlMedia,
                timeSend = timestamp,
                status = "Đã gửi"
            )

            // Lưu tin nhắn vào Realtime Database
            try {
                messagesRef.child("$finalConversationId/$messageId").setValue(messageData).await()
            } catch (e: Exception) {
                return ReturnResult.Error("Lỗi lưu tin nhắn!")
            }

            // Cập nhật lastMessage vào Firestore
            try {
                val lastMessageData = mapOf("lastMessage" to messageData)
                conversationsRef.document(finalConversationId).update(lastMessageData).await()
            } catch (e: Exception) {
                // Rollback dữ liệu nếu cập nhật Firestore thất bại
                messagesRef.child("$finalConversationId/$messageId").removeValue().await()
                return ReturnResult.Error("Lỗi tin nhắn!")
            }

            return ReturnResult.Success(
                Conversation(
                    finalConversationId,
                    isGroup,
                    null,
                    null,
                    null,
                    messageData
                )
            )

        } catch (e: Exception) {
            return ReturnResult.Error("Gửi tin nhắn thất bại!")
        }
    }

    suspend fun getConversationIdForOneToOne(
        currentUID: String,
        receiverUID: String
    ): String? {
        val conversationsRef = firestore.collection(COLLECTION_PATH_CONVERSATION)
        return try {
            val querySnapshot = conversationsRef
                .whereEqualTo("group", false)
                .whereArrayContains("listIdChatPerson", currentUID)
                .get()
                .await()

            querySnapshot.documents.firstOrNull { doc ->
                val listIdChatPerson = doc.get("listIdChatPerson") as? List<*>
                listIdChatPerson?.contains(receiverUID) == true
            }?.id
        } catch (e: Exception) {
            null
        }
    }

    fun getMessages(conversationId: String): Flow<ReturnResult<List<Message>>> = callbackFlow {
        val messagesRef = realtimeDB.getReference("$COLLECTION_PATH_MESSAGE/$conversationId")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val messages = snapshot.children.mapNotNull { child ->
                        child.getValue(Message::class.java)
                    }.sortedBy { it.timeSend }
                    trySend(ReturnResult.Success(messages))
                } catch (e: Exception) {
                    trySend(ReturnResult.Error(e.message ?: "Lỗi xử lý dữ liệu"))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(ReturnResult.Error(error.message)) // Đóng luồng nếu có lỗi
            }
        }

        messagesRef.addValueEventListener(listener)

        // Loại bỏ listener khi không còn sử dụng
        awaitClose { messagesRef.removeEventListener(listener) }
    }.flowOn(Dispatchers.IO)
        .retry(3) { cause ->
            cause is IOException
        }
        .catch { e ->
            emit(ReturnResult.Error(e.message ?: "Network error occurred"))
        }
}