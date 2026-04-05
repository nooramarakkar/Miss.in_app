package com.missin.app.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.missin.app.data.model.FoundItem
import com.missin.app.data.model.LostItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FirebaseFirestore

@HiltWorker
class MatchCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: return Result.success()
        val prefs = context.getSharedPreferences("missin_prefs", Context.MODE_PRIVATE)
        val lastChecked = prefs.getLong("last_match_check", System.currentTimeMillis() - 24 * 60 * 60 * 1000)

        var matchFound = false

        try {
            val userLostItems = firestore.collection("lost_items")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "active")
                .get().await().toObjects(LostItem::class.java)

            val newlyFoundQuery = firestore.collection("found_items")
                .whereGreaterThan("timestamp", lastChecked)
                .whereEqualTo("status", "active")
                .get().await().toObjects(FoundItem::class.java)
            
            for (lost in userLostItems) {
                if (matchFound) break
                val lat = lost.location?.latitude ?: continue
                val lng = lost.location?.longitude ?: continue
                
                for (found in newlyFoundQuery) {
                    if (lost.category == found.category && found.userId != uid) {
                        val distanceResults = FloatArray(1)
                        val flong = found.location?.longitude ?: continue
                        val flat = found.location?.latitude ?: continue
                        Location.distanceBetween(lat, lng, flat, flong, distanceResults)
                        if (distanceResults[0] <= 5000f) {
                            matchFound = true
                            break
                        }
                    }
                }
            }

            if (!matchFound) {
                val userFoundItems = firestore.collection("found_items")
                    .whereEqualTo("userId", uid)
                    .whereEqualTo("status", "active")
                    .get().await().toObjects(FoundItem::class.java)

                val newlyLostQuery = firestore.collection("lost_items")
                    .whereGreaterThan("timestamp", lastChecked)
                    .whereEqualTo("status", "active")
                    .get().await().toObjects(LostItem::class.java)

                for (found in userFoundItems) {
                    if (matchFound) break
                    val lat = found.location?.latitude ?: continue
                    val lng = found.location?.longitude ?: continue
                    for (lost in newlyLostQuery) {
                        if (found.category == lost.category && lost.userId != uid) {
                            val distanceResults = FloatArray(1)
                            val llong = lost.location?.longitude ?: continue
                            val llat = lost.location?.latitude ?: continue
                            Location.distanceBetween(lat, lng, llat, llong, distanceResults)
                            if (distanceResults[0] <= 5000f) {
                                matchFound = true
                                break
                            }
                        }
                    }
                }
            }
            
            if (matchFound) {
                showMatchNotification(context)
            }

            prefs.edit().putLong("last_match_check", System.currentTimeMillis()).apply()
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun showMatchNotification(context: Context) {
        val channelId = "match_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Match Alerts", NotificationManager.IMPORTANCE_HIGH)
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New Potential Match!")
            .setContentText("A new item matches your report. Open MissIn to check.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } else {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}
