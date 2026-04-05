package com.missin.app.data.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val content: String = "",
    val type: String = "text", // text, image, reveal_request, reveal_approved
    val timestamp: Long = System.currentTimeMillis()
)
