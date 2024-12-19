package com.lebatinh.messenger.user

class Validator {
    fun isValidEmail(email: String): Boolean {
        if (email.isEmpty() || email.length > 30) {
            return false
        }

        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        return email.matches(Regex(emailRegex))
    }

    fun isValidPassword(password: String): Boolean {
        if (password.length < 6 || password.length > 20) {
            return false
        }

        val passwordRegex = "^[A-Za-z0-9@.*#]+$"
        return password.matches(Regex(passwordRegex))
    }

    fun isValidPhoneNumber(phone: String): Boolean {
        val phoneRegex = "^\\+?[0-9]{4,14}$"
        return (phone != "") && phone.matches(Regex(phoneRegex))
    }

    fun isValidRePassword(password: String, repassword: String): Boolean {
        if (password.length < 6 || password.length > 20 || repassword.length < 6 || repassword.length > 20) {
            return false
        }
        val passwordRegex = "^[A-Za-z0-9@.*#]+$"
        return (password.matches(Regex(passwordRegex)) && password == repassword)
    }
}