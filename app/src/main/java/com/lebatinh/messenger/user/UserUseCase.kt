package com.lebatinh.messenger.user

import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.lebatinh.messenger.other.ReturnResult
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend fun register(email: String, password: String): ReturnResult<User> {
        return try {
            val authResult =
                repository.firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val userUID = authResult.user?.uid
                ?: return ReturnResult.Error("Cannot retrieve user information")
            val user = User(userUID = userUID, email = email)

            val saveResult = repository.register(user)
            if (saveResult is ReturnResult.Success) {
                ReturnResult.Success(user)
            } else {
                saveResult as ReturnResult.Error
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            ReturnResult.Error("Email đã được đăng ký!")
        } catch (e: Exception) {
            ReturnResult.Error("Có lỗi xảy ra! Hãy thử lại sau!")
        }
    }

    suspend fun login(email: String, password: String): ReturnResult<User> {
        val result = repository.login(email, password)
        return if (result is ReturnResult.Success) {
            val firebaseUser = result.data
            val user = User(userUID = firebaseUser.uid, email = firebaseUser.email ?: "")
            ReturnResult.Success(user)
        } else {
            ReturnResult.Error("Đăng nhập thất bại! Hãy kiểm tra lại tài khoản và mật khẩu.")
        }
    }

    suspend fun forgotPass(email: String, name: String): ReturnResult<Unit> {
        val userExistsResult = repository.checkIfUserExists(email)
        if (userExistsResult is ReturnResult.Success && userExistsResult.data) {
            val fullNameResult = repository.getUserFullName(email)
            if (fullNameResult is ReturnResult.Success && fullNameResult.data == name) {
                return repository.sendResetPasswordEmail(email)
            }
        }
        return ReturnResult.Error("Thông tin tài khoản hoặc tên không chính xác!")
    }

    suspend fun changePass(newPassword: String): ReturnResult<Unit> {
        return repository.changePassword(newPassword)
    }

    suspend fun updateUserInfo(
        email: String,
        fullName: String?,
        phoneNumber: String?,
        birthday: String?,
        avatar: String?
    ): ReturnResult<Unit> {
        val updates = mutableMapOf<String, Any>()
        fullName?.let { updates["fullName"] = it }
        phoneNumber?.let { updates["phoneNumber"] = it }
        birthday?.let { updates["birthday"] = it }
        avatar?.let { updates["avatar"] = it }

        // Kiểm tra trùng lặp số điện thoại nếu được truyền vào
        if (!phoneNumber.isNullOrEmpty()) {
            val phoneCheckResult = repository.isPhoneNumberUsed(phoneNumber)
            if (phoneCheckResult is ReturnResult.Success && phoneCheckResult.data) {
                return ReturnResult.Error("Số điện thoại đã được sử dụng!")
            } else if (phoneCheckResult is ReturnResult.Error) {
                return ReturnResult.Error(phoneCheckResult.message)
            }
        }

        return if (updates.isNotEmpty()) {
            repository.updateUserInfo(email, updates)
        } else {
            ReturnResult.Error("Không có gì để cập nhật!")
        }
    }

    suspend fun searchUsers(query: String, currentUserUID: String): ReturnResult<List<User>> {
        val result = repository.searchUsers(query, currentUserUID)

        if (result is ReturnResult.Success) {
            return result
        }
        return ReturnResult.Error("Lỗi tìm kiếm!")
    }

    suspend fun getUserByUID(userUID: String): ReturnResult<User> {
        return repository.getUserByUID(userUID)
    }

    suspend fun getInfoUserByUID(userUID: String): User? {
        return repository.getInfoUserByUID(userUID)
    }
}