package com.lebatinh.messenger.helper

import com.lebatinh.messenger.Key_Password.EMAIL_SENDER
import com.lebatinh.messenger.Key_Password.PASSWORD_SENDER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class GmailHelper {

    /**
     * Gửi email OTP cho người dùng
     * @param otp : otp gửi đi
     * @param receiverEmail : email người nhận
     * @param onResult : kết quả trả về
     */
    fun sendEmailOTP(
        otp: String,
        receiverEmail: String,
        onResult: (Boolean, String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val senderEmail = EMAIL_SENDER
            val senderPassword = PASSWORD_SENDER
            val senderName = "Admin Messenger"

            val body = "OTP của bạn là $otp. Mã sẽ hết hạn sau 3 phút"
            val subject = "OTP xác thực cho ứng dụng Messenger"

            // Thiết lập thông số SMTP
            val properties = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", "smtp.gmail.com")
                put("mail.smtp.port", "587")
            }

            val session = Session.getInstance(properties, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(senderEmail, senderPassword)
                }
            })

            try {
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(senderEmail, senderName))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(receiverEmail))
                    this.subject = subject
                    setText(body)
                }

                // Gửi email
                Transport.send(message)
                onResult(true, "")
            } catch (e: MessagingException) {
                e.printStackTrace()
                onResult(false, e.toString())
            }
        }
    }
}