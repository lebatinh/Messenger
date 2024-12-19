package com.lebatinh.messenger.notification

data class NotiData(
    val title: String,
    val message: String,
    val data: Map<String, Any> = emptyMap()
)