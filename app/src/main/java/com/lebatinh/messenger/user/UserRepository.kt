package com.lebatinh.messenger.user

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.lebatinh.messenger.Key_Password.COLLECTION_PATH_USER
import com.lebatinh.messenger.other.ReturnResult
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val userCollection = FirebaseFirestore.getInstance().collection(COLLECTION_PATH_USER)

    /**
     * Đăng ký tài khoản, mật khẩu và thông tin cơ bản của người dùng
     */
    suspend fun register(user: User): ReturnResult<User> {
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid
        val email = FirebaseAuth.getInstance().currentUser?.email

        return if (email != null && currentUser != null) {
            val updatedUser = user.copy(userUID = currentUser, email = email)
            try {
                userCollection.document(email).set(updatedUser).await()
                ReturnResult.Success(updatedUser)
            } catch (e: Exception) {
                Log.d("error", e.toString())
                ReturnResult.Error("Lỗi không xác định!")
            }
        } else {
            ReturnResult.Error("Không thể lấy thông tin người dùng.")
        }
    }

    /**
     * Login với:
     * @param email
     * @param password
     */
    suspend fun login(email: String, password: String): ReturnResult<User> {
        return try {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            firebaseUser?.let {
                val user = convertToUser(it)
                ReturnResult.Success(user)
            } ?: ReturnResult.Error("Không thể lấy thông tin người dùng.")
        } catch (e: Exception) {
            Log.d("error", e.toString())
            ReturnResult.Error("Đăng nhập thất bại!")
        }
    }

    /**
     * Kiểm tra tồn tại của người dùng với:
     * @param email
     * @param name
     */
    suspend fun checkIfUserExists(email: String, name: String): ReturnResult<Boolean> {
        return try {
            val document = userCollection.document(email).get().await()
            if (document.exists()) {
                val userFullName = document.getString("fullName")
                ReturnResult.Success(userFullName == name)
            } else {
                ReturnResult.Error("Tài khoản email hoặc tên không chính xác!")
            }
        } catch (e: Exception) {
            Log.d("error", e.toString())
            ReturnResult.Error("Lỗi khi kiểm tra tài khoản.")
        }
    }

    /**
     * Đổi mật khẩu của currentUser với:
     * @param newPassword
     */
    suspend fun changePassword(newPassword: String): ReturnResult<Unit> {
        val currentUser = FirebaseAuth.getInstance().currentUser
        return if (currentUser != null) {
            try {
                currentUser.updatePassword(newPassword).await()
                ReturnResult.Success(Unit)
            } catch (e: Exception) {
                Log.d("error", e.toString())
                ReturnResult.Error("Không thể đổi mật khẩu. Vui lòng thử lại sau.")
            }
        } else {
            ReturnResult.Error("Hiện tại không thể đổi mật khẩu.")
        }
    }

    /**
     * Gửi email đặt lại mật khẩu với:
     * @param email
     */
    suspend fun sendResetPasswordEmail(email: String): ReturnResult<Unit> {
        return try {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
            ReturnResult.Success(Unit)
        } catch (e: Exception) {
            Log.d("error", e.toString())
            ReturnResult.Error("Không thể gửi email đặt lại mật khẩu. Vui lòng thử lại sau.")
        }
    }

    /**
     * cập nhật với:
     * @param email
     * @param fullName
     * @param phoneNumber
     * @param avatar
     */
    suspend fun updateUserInfo(
        email: String,
        fullName: String?,
        phoneNumber: String?,
        birthday: String?,
        avatar: String?
    ): ReturnResult<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>()

            fullName?.let { updates["fullName"] = it }
            phoneNumber?.let { updates["phoneNumber"] = it }
            birthday?.let { updates["birthday"] = it }
            avatar?.let { updates["avatar"] = it }

            // Nếu có dữ liệu cập nhật, thực hiện cập nhật
            if (updates.isNotEmpty()) {
                userCollection.document(email).update(updates).await()
            }
            ReturnResult.Success(Unit)
        } catch (e: Exception) {
            Log.d("error", e.toString())
            ReturnResult.Error("Lỗi khi cập nhật thông tin.")
        }
    }

    private fun convertToUser(firebaseUser: FirebaseUser): User {
        return User(
            userUID = firebaseUser.uid,
            email = firebaseUser.email ?: ""
        )
    }

    /**
     * Lấy thông tin người dùng với:
     * @param email
     */
    suspend fun getUserInfo(email: String): ReturnResult<User> {
        return try {
            val snapshot = userCollection.document(email).get().await()
            val user = snapshot.toObject(User::class.java)
            if (user != null) {
                ReturnResult.Success(user)
            } else {
                ReturnResult.Error("Không có thông tin người dùng.")
            }
        } catch (e: Exception) {
            Log.d("error", e.toString())
            ReturnResult.Error("Có lỗi xảy ra!")
        }
    }
}
