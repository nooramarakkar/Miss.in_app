package com.missin.app.data.model

data class Notification(
    val id: String = "",
    val reportId: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
