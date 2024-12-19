package com.lebatinh.messenger.other

sealed class NotificationType(val type: String) {
    object Message : NotificationType("message")
    object GroupMessage : NotificationType("group_message")
    object FriendRequest : NotificationType("friend_request")
    object System : NotificationType("system")
}