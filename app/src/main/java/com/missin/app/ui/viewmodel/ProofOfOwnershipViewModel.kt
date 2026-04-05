package com.missin.app.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.missin.app.data.model.ClaimRequest
import com.missin.app.data.repository.ClaimRepository
import com.missin.app.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProofOfOwnershipViewModel @Inject constructor(
    private val claimRepository: ClaimRepository,
    private val itemRepository: ItemRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess = _isSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    /**
     * Entry point called from ProofOfOwnershipScreen.
     * @param foundItemId  – the document ID of the found item
     * @param finderId     – the userId of the person who reported/found the item (comes from FoundItem.userId)
     * @param description  – user's proof description
     * @param imageUri     – optional proof image URI
     * @param context      – Android context for file conversion
     */
    fun submitClaim(
        foundItemId: String,
        finderId: String,
        description: String,
        imageUri: Uri?,
        context: android.content.Context
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val claimerId = auth.currentUser?.uid
            if (claimerId == null) {
                _errorMessage.value = "You must be logged in to submit a claim."
                _isLoading.value = false
                return@launch
            }

            if (imageUri != null) {
                val file = com.missin.app.utils.FileUtils.uriToFile(context, imageUri)
                if (file != null) {
                    itemRepository.uploadImageToCloudinary(file) { url ->
                        if (url != null) {
                            proceedWithClaim(foundItemId, finderId, claimerId, description, url)
                        } else {
                            _errorMessage.value = "Failed to upload proof image"
                            _isLoading.value = false
                        }
                    }
                } else {
                    _errorMessage.value = "Failed to process image file"
                    _isLoading.value = false
                }
            } else {
                proceedWithClaim(foundItemId, finderId, claimerId, description, "")
            }
        }
    }

    private fun proceedWithClaim(
        itemId: String,
        finderId: String,
        claimerId: String,
        description: String,
        proofImageUrl: String
    ) {
        viewModelScope.launch {
            // STEP 2 FIX: claimerId = current user UID, finderId = item reporter's userId
            val claim = ClaimRequest(
                itemId = itemId,
                claimerId = claimerId,   // was claimantId — this was the Firestore key mismatch bug
                finderId = finderId,
                proofDescription = description,
                proofImageUrl = proofImageUrl
            )

            // Debug log so Logcat confirms the correct values before the write
            Log.d("ClaimDebug", "Uploading claim: Claimer=$claimerId, Finder=$finderId, Item=$itemId")

            val result = claimRepository.submitClaimRequest(claim)
            if (result.isSuccess) {
                _isSuccess.value = true
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e("ClaimDebug", "Claim submission failed", result.exceptionOrNull())
            }
            _isLoading.value = false
        }
    }
}
