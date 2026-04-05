package com.missin.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.missin.app.data.model.FoundItem
import com.missin.app.data.model.LostItem
import com.missin.app.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.firebase.firestore.GeoPoint
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ReportFormsViewModel @Inject constructor(
    private val repository: ItemRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess = _isSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Notice I changed imageUri: Uri? to imageFile: File? to match your Cloudinary setup
    fun submitLostReport(title: String, category: String, latitude: Double, longitude: Double, address: String, description: String, imageFile: File?) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val uid = auth.currentUser?.uid
            if (uid == null) {
                _errorMessage.value = "User not logged in."
                _isLoading.value = false
                return@launch
            }

            if (imageFile != null) {
                repository.uploadImageToCloudinary(imageFile) { cloudinaryUrl ->
                    if (cloudinaryUrl != null) {
                        saveLostItemToFirestore(uid, title, category, latitude, longitude, address, description, cloudinaryUrl)
                    } else {
                        _errorMessage.value = "Failed to upload image."
                        _isLoading.value = false
                    }
                }
            } else {
                saveLostItemToFirestore(uid, title, category, latitude, longitude, address, description, "")
            }
        }
    }

    // Helper function to handle the Firestore save cleanly
    private fun saveLostItemToFirestore(uid: String, title: String, category: String, latitude: Double, longitude: Double, address: String, description: String, imageUrl: String) {
        viewModelScope.launch {
            val item = LostItem(
                userId = uid,
                title = title,
                itemType = "lost",
                category = category,
                address = address,
                location = GeoPoint(latitude, longitude),
                description = description,
                imageUrl = imageUrl
            )

            val result = repository.reportLostItem(item)
            if (result.isSuccess) {
                _isSuccess.value = true
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Unknown error saving to database"
            }
            _isLoading.value = false
        }
    }

    // Notice again: changing imageUri: Uri? to imageFile: File?
    fun submitFoundReport(title: String, category: String, latitude: Double, longitude: Double, address: String, description: String, imageFile: File?) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val uid = auth.currentUser?.uid
            if (uid == null) {
                _errorMessage.value = "User not logged in."
                _isLoading.value = false
                return@launch
            }

            if (imageFile != null) {
                repository.uploadImageToCloudinary(imageFile) { cloudinaryUrl ->
                    if (cloudinaryUrl != null) {
                        saveFoundItemToFirestore(uid, title, category, latitude, longitude, address, description, cloudinaryUrl)
                    } else {
                        _errorMessage.value = "Failed to upload image."
                        _isLoading.value = false
                    }
                }
            } else {
                saveFoundItemToFirestore(uid, title, category, latitude, longitude, address, description, "")
            }
        }
    }

    // Helper function for Found items
    private fun saveFoundItemToFirestore(uid: String, title: String, category: String, latitude: Double, longitude: Double, address: String, description: String, imageUrl: String) {
        viewModelScope.launch {
            val item = FoundItem(
                userId = uid,
                title = title,
                itemType = "found",
                category = category,
                address = address,
                location = GeoPoint(latitude, longitude),
                description = description,
                imageUrl = imageUrl
            )

            val result = repository.reportFoundItem(item)
            if (result.isSuccess) {
                _isSuccess.value = true
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Unknown error saving to database"
            }
            _isLoading.value = false
        }
    }
}
