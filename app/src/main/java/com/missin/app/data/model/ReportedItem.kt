package com.missin.app.data.model

data class ReportedItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val username: String = "",
    val phoneNumber: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)