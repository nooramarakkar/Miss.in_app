package com.missin.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.missin.app.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class KarmaViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    fun loadUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(uid).get().await()
                if (snapshot.exists()) {
                    _userProfile.value = snapshot.toObject(User::class.java)
                }
            } catch (e: Exception) {
                // Ignore for now
            }
        }
    }
}
