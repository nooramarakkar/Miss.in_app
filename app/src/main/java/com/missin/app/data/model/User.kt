package com.missin.app.data.model

data class User(
    val uid: String = "",
    val phoneNumber: String = "",
    val name: String = "",
    val email: String? = null,
    val homeLocation: com.google.firebase.firestore.GeoPoint? = null,
    val karma: Long = 0L,
    val joinedAt: Long = System.currentTimeMillis()
)
