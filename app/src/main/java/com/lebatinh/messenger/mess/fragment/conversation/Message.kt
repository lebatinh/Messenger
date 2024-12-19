package com.lebatinh.messenger.mess.fragment.conversation

import com.lebatinh.messenger.other.MessageType

data class Message(
    val id: String? = null,
    val senderId: String? = null,
    val type: MessageType = MessageType.TEXT,
    val message: String? = null,
    val urlMedia: List<String>? = null,
    val timeSend: Long? = null,
    val status: String? = null
)