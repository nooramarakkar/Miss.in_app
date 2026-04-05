package com.missin.app.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.missin.app.data.model.FoundItem
import com.missin.app.data.model.LostItem
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.missin.app.data.model.User
import com.missin.app.data.model.Notification
import android.location.Location
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    suspend fun uploadImage(uri: Uri): Result<String> {
        return try {
            val fileName = UUID.randomUUID().toString()
            val ref = storage.reference.child("images/$fileName")
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportLostItem(item: LostItem): Result<Unit> {
        return try {
            val docRef = firestore.collection("lost_items").document()
            val itemWithId = item.copy(id = docRef.id)
            docRef.set(itemWithId).await()
            itemWithId.location?.let { geo ->
                submitProximityNotifications(
                    itemType = "lost",
                    reportId = itemWithId.id,
                    title = itemWithId.title,
                    description = itemWithId.description,
                    imageUrl = itemWithId.imageUrl,
                    lat = geo.latitude,
                    lng = geo.longitude
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportFoundItem(item: FoundItem): Result<Unit> {
        return try {
            val docRef = firestore.collection("found_items").document()
            val itemWithId = item.copy(id = docRef.id)
            docRef.set(itemWithId).await()
            itemWithId.location?.let { geo ->
                submitProximityNotifications(
                    itemType = "found",
                    reportId = itemWithId.id,
                    title = itemWithId.title,
                    description = itemWithId.description,
                    imageUrl = itemWithId.imageUrl,
                    lat = geo.latitude,
                    lng = geo.longitude
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findMatchesForLostItem(category: String): Result<List<FoundItem>> {
        return try {
            val snapshot = firestore.collection("found_items")
                .whereEqualTo("category", category)
                .whereEqualTo("status", "active")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val matches = snapshot.toObjects(FoundItem::class.java)
            Result.success(matches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findMatchesForFoundItem(category: String): Result<List<LostItem>> {
        return try {
            val snapshot = firestore.collection("lost_items")
                .whereEqualTo("category", category)
                .whereEqualTo("status", "active")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val matches = snapshot.toObjects(LostItem::class.java)
            Result.success(matches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecentLostItems(limit: Long = 20): Result<List<LostItem>> {
        return try {
            val snapshot = firestore.collection("lost_items")
                .whereEqualTo("status", "active")
                .get()
                .await()
            val sorted = snapshot.toObjects(LostItem::class.java)
                .sortedByDescending { it.timestamp }
                .take(limit.toInt())
            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecentFoundItems(limit: Long = 20): Result<List<FoundItem>> {
        return try {
            val snapshot = firestore.collection("found_items")
                .whereEqualTo("status", "active")
                .get()
                .await()
            val sorted = snapshot.toObjects(FoundItem::class.java)
                .sortedByDescending { it.timestamp }
                .take(limit.toInt())
            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun uploadImageToCloudinary(file: File, onResult: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MediaManager.get().upload(file.path)
                    .unsigned("missin_unsigned")
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String?) {}
                        override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                        override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                            val url = resultData["secure_url"] as? String
                            onResult(url)
                        }
                        override fun onError(requestId: String?, error: ErrorInfo?) {
                            onResult(null)
                        }
                        override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                    }).dispatch()
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    private suspend fun submitProximityNotifications(
        itemType: String, reportId: String, title: String, description: String, imageUrl: String, lat: Double, lng: Double
    ) {
        try {
            val latOffset = 10.0 / 111.32
            val minLat = lat - latOffset
            val maxLat = lat + latOffset

            val usersSnapshot = firestore.collection("users")
                .whereGreaterThan("homeLocation.latitude", minLat)
                .whereLessThan("homeLocation.latitude", maxLat)
                .get()
                .await()

            val users = usersSnapshot.toObjects(User::class.java)

            for (user in users) {
                user.homeLocation?.let { home ->
                    val results = FloatArray(1)
                    Location.distanceBetween(lat, lng, home.latitude, home.longitude, results)
                    if (results[0] <= 10000f) {
                        val notification = Notification(
                            id = UUID.randomUUID().toString(),
                            reportId = reportId,
                            userId = user.uid,
                            title = "Nearby ${itemType.replaceFirstChar { it.uppercase() }}: $title",
                            description = description,
                            imageUrl = imageUrl,
                            latitude = lat,
                            longitude = lng
                        )
                        firestore.collection("users").document(user.uid)
                            .collection("incoming_notifications").document(notification.id)
                            .set(notification)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getUserReports(userId: String): Result<List<Any>> {
        return try {
            val lostSnapshot = firestore.collection("lost_items")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                
            val foundSnapshot = firestore.collection("found_items")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                
            val lostItems = lostSnapshot.toObjects(LostItem::class.java)
            val foundItems = foundSnapshot.toObjects(FoundItem::class.java)
            
            val combined = (lostItems + foundItems).sortedByDescending { item ->
                when (item) {
                    is LostItem -> item.timestamp
                    is FoundItem -> item.timestamp
                    else -> 0L
                }
            }
            
            Result.success(combined)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
