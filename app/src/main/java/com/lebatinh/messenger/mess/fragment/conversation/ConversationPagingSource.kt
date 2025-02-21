package com.lebatinh.messenger.mess.fragment.conversation

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.lebatinh.messenger.Key_Password.COLLECTION_PATH_CONVERSATION
import com.lebatinh.messenger.Key_Password.PAGE_SIZE
import kotlinx.coroutines.tasks.await

class ConversationPagingSource(
    private val firestore: FirebaseFirestore,
    private val currentUID: String,
    private val isGroup: Boolean? = null
) : PagingSource<DocumentSnapshot, Conversation>() {

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, Conversation>): DocumentSnapshot? {
        return null
    }

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, Conversation> {
        return try {
            // Tạo query cơ bản
            var query = firestore.collection(COLLECTION_PATH_CONVERSATION)
                .whereArrayContains("listIdChatPerson", currentUID)
//                .orderBy("lastMessage.timeSend", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE.toLong())

            // Thêm điều kiện group nếu có
            query = when (isGroup) {
                true -> query.whereEqualTo("group", true)
                false -> query.whereEqualTo("group", false)
                else -> query
            }

            // Lấy snapshot
            val currentPage = if (params.key != null) {
                query.startAfter(params.key!!).get().await()
            } else {
                query.get().await()
            }

            // Chuyển đổi documents thành conversations
            val conversations = currentPage.documents.mapNotNull { doc ->
                doc.toObject(Conversation::class.java)
            }

            // Xác định key cho trang tiếp theo
            val lastDocument = currentPage.documents.lastOrNull()

            LoadResult.Page(
                data = conversations,
                prevKey = null,
                nextKey = if (conversations.size < PAGE_SIZE) null else lastDocument
            )

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}