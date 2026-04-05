package com.missin.app.data.model

data class ClaimRequest(
    val id: String = "",
    val itemId: String = "",          // ID of the found item being claimed
    val claimerId: String = "",       // UID of the person making the claim (was: claimantId — BUG)
    val finderId: String = "",        // UID of the person who reported the item
    val proofDescription: String = "",
    val proofImageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "pending",   // pending, active (chat), resolved, declined
    val callRequested: Boolean = false,
    val callAccepted: Boolean = false
)
