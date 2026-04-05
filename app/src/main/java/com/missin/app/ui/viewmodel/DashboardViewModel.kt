package com.missin.app.ui.viewmodel

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.missin.app.data.model.User
import com.missin.app.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _potentialMatches = MutableStateFlow<List<Any>>(emptyList())
    val potentialMatches: StateFlow<List<Any>> = _potentialMatches.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _recentLostItems = MutableStateFlow<List<com.missin.app.data.model.LostItem>>(emptyList())
    val recentLostItems: StateFlow<List<com.missin.app.data.model.LostItem>> = _recentLostItems.asStateFlow()

    private val _recentFoundItems = MutableStateFlow<List<com.missin.app.data.model.FoundItem>>(emptyList())
    val recentFoundItems: StateFlow<List<com.missin.app.data.model.FoundItem>> = _recentFoundItems.asStateFlow()

    init {
        loadUserProfile()
        loadRecentFeeds()
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _errorMessage.value = "User not logged in."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        
        firestore.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                _errorMessage.value = error.localizedMessage ?: "Failed to fetch user profile."
                _isLoading.value = false
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                if (user != null) {
                    _userProfile.value = user
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Failed to parse user profile."
                }
            } else {
                _errorMessage.value = "User profile not found."
            }
            _isLoading.value = false
        }

    }

    fun loadRecentFeeds() {
        viewModelScope.launch {
            val lostResult = itemRepository.getRecentLostItems()
            lostResult.onSuccess { _recentLostItems.value = it }
            
            val foundResult = itemRepository.getRecentFoundItems()
            foundResult.onSuccess { _recentFoundItems.value = it }
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun loadMatchesForLostItem(category: String, latitude: Double, longitude: Double, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = itemRepository.findMatchesForLostItem(category)
            result.onSuccess { matches ->
                val distanceResults = FloatArray(1)
                val filteredMatches = matches.filter { foundItem ->
                    foundItem.location?.let { loc ->
                        Location.distanceBetween(latitude, longitude, loc.latitude, loc.longitude, distanceResults)
                        val within5Km = distanceResults[0] <= 5000f
                        val within30Days = Math.abs(foundItem.timestamp - timestamp) <= 30L * 24 * 60 * 60 * 1000
                        within5Km && within30Days
                    } ?: false
                }
                
                _potentialMatches.value = filteredMatches
                if (filteredMatches.isNotEmpty()) {
                    showMatchNotification("Potential Match Found!", "We found ${filteredMatches.size} item(s) within 5km matching your loss.")
                }
            }
            _isLoading.value = false
        }
    }

    fun loadMatchesForFoundItem(category: String, latitude: Double, longitude: Double, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = itemRepository.findMatchesForFoundItem(category)
            result.onSuccess { matches ->
                val distanceResults = FloatArray(1)
                val filteredMatches = matches.filter { lostItem ->
                    lostItem.location?.let { loc ->
                        Location.distanceBetween(latitude, longitude, loc.latitude, loc.longitude, distanceResults)
                        val within5Km = distanceResults[0] <= 5000f
                        val within30Days = Math.abs(lostItem.timestamp - timestamp) <= 30L * 24 * 60 * 60 * 1000
                        within5Km && within30Days
                    } ?: false
                }
                
                _potentialMatches.value = filteredMatches
                if (filteredMatches.isNotEmpty()) {
                    showMatchNotification("Potential Match Found!", "We found ${filteredMatches.size} item(s) within 5km matching what you found.")
                }
            }
            _isLoading.value = false
        }
    }

    private fun showMatchNotification(title: String, message: String) {
        val channelId = "match_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Match Alerts", NotificationManager.IMPORTANCE_HIGH)
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
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
