package com.lebatinh.messenger.mess.fragment.conversation

data class Conversation(
    val conversationId: String? = null,
    val group: Boolean? = false,
    val imageGroup: String? = null,
    val listIdChatPerson: List<String>? = emptyList(),
    val conversationName: String? = null,
    val lastMessage: Message? = null
)