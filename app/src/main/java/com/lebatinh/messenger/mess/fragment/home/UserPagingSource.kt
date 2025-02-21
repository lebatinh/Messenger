package com.lebatinh.messenger.mess.fragment.home

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.lebatinh.messenger.Key_Password.COLLECTION_PATH_USER
import com.lebatinh.messenger.Key_Password.PAGE_SIZE
import com.lebatinh.messenger.user.User
import kotlinx.coroutines.tasks.await

class UserPagingSource(
    private val firestore: FirebaseFirestore,
    private val query: String,
    private val currentUserUID: String
) : PagingSource<DocumentSnapshot, User>() {

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, User>): DocumentSnapshot? {
        return null
    }

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, User> {
        return try {
            val results = mutableListOf<User>()

            // 1. Search by userUID
            val userUIDQuery = firestore.collection(COLLECTION_PATH_USER)
                .whereEqualTo("userUID", query)
                .limit(PAGE_SIZE.toLong())

            val userUIDSnapshot = if (params.key == null) {
                userUIDQuery.get().await()
            } else {
                userUIDQuery.startAfter(params.key!!).get().await()
            }

            results.addAll(userUIDSnapshot.documents.mapNotNull {
                it.toObject(User::class.java)
            })

            // 2. Search by phone number if no results found
            if (results.isEmpty()) {
                val phoneQuery = firestore.collection(COLLECTION_PATH_USER)
                    .whereEqualTo("phoneNumber", query)
                    .limit(PAGE_SIZE.toLong())

                val phoneSnapshot = if (params.key == null) {
                    phoneQuery.get().await()
                } else {
                    phoneQuery.startAfter(params.key!!).get().await()
                }

                results.addAll(phoneSnapshot.documents.mapNotNull {
                    it.toObject(User::class.java)
                })
            }

            // 3. Search by name if still no results found
            if (results.isEmpty()) {
                val nameQuery = firestore.collection(COLLECTION_PATH_USER)
                    .whereGreaterThanOrEqualTo("fullName", query)
                    .whereLessThan("fullName", query + '\uf8ff')
                    .limit(PAGE_SIZE.toLong())

                val nameSnapshot = if (params.key == null) {
                    nameQuery.get().await()
                } else {
                    nameQuery.startAfter(params.key!!).get().await()
                }

                results.addAll(nameSnapshot.documents.mapNotNull {
                    it.toObject(User::class.java)
                })
            }

            // Filter out current user
            val filteredResults = results.filter { it.userUID != currentUserUID }

            // Get the last document for next page
            val lastDocument = when {
                userUIDSnapshot.documents.isNotEmpty() -> userUIDSnapshot.documents.last()
                results.isNotEmpty() -> results.last().let { lastUser ->
                    firestore.collection(COLLECTION_PATH_USER)
                        .whereEqualTo("userUID", lastUser.userUID)
                        .get().await().documents.firstOrNull()
                }

                else -> null
            }

            LoadResult.Page(
                data = filteredResults,
                prevKey = null,
                nextKey = if (filteredResults.size < PAGE_SIZE) null else lastDocument
            )

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}