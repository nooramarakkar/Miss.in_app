package com.missin.app.data.model

data class FoundItem(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val itemType: String = "found",
    val category: String = "",
    val description: String = "",
    val address: String = "",
    val location: com.google.firebase.firestore.GeoPoint? = null,
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "active" // active, resolved
)
