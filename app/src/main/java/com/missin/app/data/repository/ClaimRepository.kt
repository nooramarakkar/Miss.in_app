package com.missin.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.missin.app.data.model.ClaimRequest
import com.missin.app.data.model.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClaimRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun submitClaimRequest(claim: ClaimRequest): Result<Unit> {
        return try {
            val docRef = firestore.collection("claim_requests").document()
            val claimWithId = claim.copy(id = docRef.id)
            docRef.set(claimWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * STEP 3 FIX: Query ONLY on "finderId". No .orderBy() — avoids the
     * missing composite-index silent failure. Sorting is done locally.
     */
    suspend fun getIncomingClaims(uid: String): Result<List<ClaimRequest>> {
        return try {
            val snapshot = firestore.collection("claim_requests")
                .whereEqualTo("finderId", uid)          // finder sees claims sent TO them
                // REMOVED: .whereEqualTo("status", "pending")  — show all statuses so active chats still appear
                // REMOVED: .orderBy("timestamp", ...)          — no composite index needed now
                .get()
                .await()

            val sorted = snapshot.toObjects(ClaimRequest::class.java)
                .sortedByDescending { it.timestamp }    // sort locally

            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * STEP 3 FIX: Query ONLY on "claimerId" (was "claimantId" — the key mismatch).
     * No .orderBy() — sort locally.
     */
    suspend fun getOutgoingClaims(uid: String): Result<List<ClaimRequest>> {
        return try {
            val snapshot = firestore.collection("claim_requests")
                .whereEqualTo("claimerId", uid)         // was "claimantId" — this caused zero results
                // REMOVED: .whereEqualTo("status", "pending")
                // REMOVED: .orderBy("timestamp", ...)
                .get()
                .await()

            val sorted = snapshot.toObjects(ClaimRequest::class.java)
                .sortedByDescending { it.timestamp }    // sort locally

            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateClaimStatus(claimId: String, status: String): Result<Unit> {
        return try {
            firestore.collection("claim_requests").document(claimId)
                .update("status", status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getClaim(claimId: String): Result<ClaimRequest?> {
        return try {
            val snapshot = firestore.collection("claim_requests").document(claimId).get().await()
            Result.success(snapshot.toObject(ClaimRequest::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMessagesForClaim(claimId: String): Flow<List<Message>> = callbackFlow {
        val listenerRegistration = firestore.collection("claim_requests")
            .document(claimId)
            .collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.toObjects(Message::class.java)
                    trySend(messages)
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun sendMessage(claimId: String, message: Message): Result<Unit> {
        return try {
            val docRef = firestore.collection("claim_requests")
                .document(claimId)
                .collection("messages")
                .document()
            docRef.set(message.copy(id = docRef.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveLoop(
        finderId: String,
        claimId: String,
        foundItemId: String,
        lostItemId: String
    ): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val finderRef = firestore.collection("users").document(finderId)
                val finderSnapshot = transaction.get(finderRef)
                val currentKarma = finderSnapshot.getLong("karma") ?: 0L
                transaction.update(finderRef, "karma", currentKarma + 10L)

                val claimRef = firestore.collection("claim_requests").document(claimId)
                transaction.update(claimRef, "status", "resolved")

                val foundRef = firestore.collection("found_items").document(foundItemId)
                transaction.update(foundRef, "status", "resolved")

                if (lostItemId.isNotBlank()) {
                    val lostRef = firestore.collection("lost_items").document(lostItemId)
                    transaction.update(lostRef, "status", "resolved")
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
