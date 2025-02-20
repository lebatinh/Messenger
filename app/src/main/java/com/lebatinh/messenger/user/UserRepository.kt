package com.lebatinh.messenger.user

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.lebatinh.messenger.Key_Password.COLLECTION_PATH_USER
import com.lebatinh.messenger.Key_Password.PAGE_SIZE
import com.lebatinh.messenger.mess.fragment.home.UserPagingSource
import com.lebatinh.messenger.other.ReturnResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    val firebaseAuth: FirebaseAuth
) {
    /**
     * Đăng ký tài khoản, mật khẩu và thông tin cơ bản của người dùng
     */
    suspend fun register(user: User): ReturnResult<Unit> {
        return try {
            firestore.collection(COLLECTION_PATH_USER).document(user.email!!).set(user).await()
            ReturnResult.Success(Unit)
        } catch (e: Exception) {
            ReturnResult.Error("Lỗi đăng ký!")
        }
    }

    /**
     * Login với:
     * @param email
     * @param password
     */
    suspend fun login(email: String, password: String): ReturnResult<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return ReturnResult.Error("User not found")
            ReturnResult.Success(firebaseUser)
        } catch (e: Exception) {
            ReturnResult.Error(e.localizedMessage ?: "Login failed")
        }
    }

    /**
     * Kiểm tra tồn tại của người dùng với:
     * @param email
     */
    suspend fun checkIfUserExists(email: String): ReturnResult<Boolean> {
        return try {
            val document = firestore.collection(COLLECTION_PATH_USER).document(email).get().await()
            ReturnResult.Success(document.exists())
        } catch (e: Exception) {
            ReturnResult.Error(e.localizedMessage ?: "Lỗi khi kiểm tra tài khoản")
        }
    }

    suspend fun getUserFullName(email: String): ReturnResult<String?> {
        return try {
            val document = firestore.collection(COLLECTION_PATH_USER).document(email).get().await()
            ReturnResult.Success(document.getString("fullName"))
        } catch (e: Exception) {
            ReturnResult.Error(e.localizedMessage ?: "Lỗi khi lấy tên đầy đủ")
        }
    }

    /**
     * Đổi mật khẩu của currentUser với:
     * @param newPassword
     */
    suspend fun changePassword(newPassword: String): ReturnResult<Unit> {
        val currentUser = firebaseAuth.currentUser
        return if (currentUser != null) {
            try {
                currentUser.updatePassword(newPassword).await()
                ReturnResult.Success(Unit)
            } catch (e: Exception) {
                ReturnResult.Error(e.localizedMessage ?: "Không thể đổi mật khẩu")
            }
        } else {
            ReturnResult.Error("Người dùng hiện tại không tồn tại.")
        }
    }

    /**
     * Gửi email đặt lại mật khẩu với:
     * @param email
     */
    suspend fun sendResetPasswordEmail(email: String): ReturnResult<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            ReturnResult.Success(Unit)
        } catch (e: Exception) {
            ReturnResult.Error(e.localizedMessage ?: "Không thể gửi email đặt lại mật khẩu")
        }
    }

    /**
     * kiểm tra tồn tại:
     * @param phoneNumber
     */
    suspend fun isPhoneNumberUsed(phoneNumber: String): ReturnResult<Boolean> {
        return try {
            val querySnapshot = firestore.collection(COLLECTION_PATH_USER)
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .await()

            // Kiểm tra xem có bất kỳ tài liệu nào khớp không
            val isUsed = !querySnapshot.isEmpty
            ReturnResult.Success(isUsed)
        } catch (e: Exception) {
            ReturnResult.Error(e.localizedMessage ?: "Lỗi khi kiểm tra số điện thoại!")
        }
    }

    /**
     * cập nhật ở:
     * @param email
     */
    suspend fun updateUserInfo(email: String, updates: Map<String, Any>): ReturnResult<Unit> {
        return try {
            firestore.collection(COLLECTION_PATH_USER).document(email).update(updates).await()
            ReturnResult.Success(Unit)
        } catch (e: Exception) {
            ReturnResult.Error(e.localizedMessage ?: "Lỗi khi cập nhật thông tin")
        }
    }

    /**
     * Tìm kiếm người dùng với:
     * @param query
     */
    fun getUsersPagingFlow(query: String, currentUserUID: String): Flow<PagingData<User>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                UserPagingSource(firestore, query, currentUserUID)
            }
        ).flow
    }

    /**
     * Lấy thông tin người dùng theo userUID
     * @param userUID
     */
    suspend fun getUserByUID(userUID: String): ReturnResult<User> {
        return try {
            val snapshot = firestore.collection(COLLECTION_PATH_USER)
                .whereEqualTo("userUID", userUID)
                .limit(1)
                .get()
                .await()

            val user = snapshot.documents.firstOrNull()?.toObject(User::class.java)
            if (user != null) {
                ReturnResult.Success(user)
            } else {
                ReturnResult.Error("Không có thông tin người dùng!")
            }
        } catch (e: Exception) {
            ReturnResult.Error(e.localizedMessage ?: "Lỗi khi lấy thông tin người dùng")
        }
    }

    /**
     * Lấy trực tiếp thông tin người dùng theo userUID
     * @param userUID
     */
    suspend fun getInfoUserByUID(userUID: String): User? {
        val snapshot = firestore.collection(COLLECTION_PATH_USER)
            .whereEqualTo("userUID", userUID)
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.toObject(User::class.java)
    }
}
