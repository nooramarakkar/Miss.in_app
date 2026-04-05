package com.missin.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.missin.app.data.model.ClaimRequest
import com.missin.app.data.model.Message
import com.missin.app.data.model.User
import com.missin.app.data.repository.AuthRepository
import com.missin.app.data.repository.ClaimRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val claimRepository: ClaimRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _claimRequest = MutableStateFlow<ClaimRequest?>(null)
    val claimRequest = _claimRequest.asStateFlow()
    
    // We fetch the other user's name directly, but for MVP we might just use redacted IDs or fetch the target user if we added getProfile
    // To keep it clean, we'll store the Target Name locally after fetching
    private val _otherUserName = MutableStateFlow<String>("User")
    val otherUserName = _otherUserName.asStateFlow()

    private val _isResolving = MutableStateFlow(false)
    val isResolving = _isResolving.asStateFlow()

    private val _isResolvedSuccess = MutableStateFlow(false)
    val isResolvedSuccess = _isResolvedSuccess.asStateFlow()

    private val _otherUserPhone = MutableStateFlow<String?>(null)
    val otherUserPhone = _otherUserPhone.asStateFlow()

    fun loadChat(claimId: String) {
        viewModelScope.launch {
            val result = claimRepository.getClaim(claimId)
            if (result.isSuccess) {
                val claim = result.getOrNull()
                _claimRequest.value = claim
                if (claim != null) {
                    val currentUid = auth.currentUser?.uid
                    val otherUserId = if (currentUid == claim.claimerId) claim.finderId else claim.claimerId
                    
                    val userSnap = firestore.collection("users").document(otherUserId).get().await()
                    val otherUser = userSnap.toObject(User::class.java)
                    if (otherUser != null) {
                        _otherUserName.value = otherUser.name
                        _otherUserPhone.value = otherUser.phoneNumber
                    }
                    
                    // Start listening to messages
                    claimRepository.getMessagesForClaim(claimId).collect { msgs ->
                        _messages.value = msgs
                    }
                }
            }
        }
    }

    fun sendMessage(content: String, type: String = "text") {
        val claimId = _claimRequest.value?.id ?: return
        val currentUid = auth.currentUser?.uid ?: return
        
        val msg = Message(
            senderId = currentUid,
            content = content,
            type = type
        )
        viewModelScope.launch {
            claimRepository.sendMessage(claimId, msg)
        }
    }

    fun requestCall() {
        val claimId = _claimRequest.value?.id ?: return
        viewModelScope.launch {
            firestore.collection("claim_requests").document(claimId)
                .update("callRequested", true).await()
            val currentClaim = _claimRequest.value
            _claimRequest.value = currentClaim?.copy(callRequested = true)
            sendMessage("", "reveal_request")
        }
    }

    fun acceptCall() {
        val claimId = _claimRequest.value?.id ?: return
        viewModelScope.launch {
            firestore.collection("claim_requests").document(claimId)
                .update("callAccepted", true).await()
            val currentClaim = _claimRequest.value
            _claimRequest.value = currentClaim?.copy(callAccepted = true)
            sendMessage("", "reveal_approved")
        }
    }

    fun resolveItem() {
        val claim = _claimRequest.value ?: return
        viewModelScope.launch {
            _isResolving.value = true
            val res = claimRepository.resolveLoop(
                finderId = claim.finderId,
                claimId = claim.id,
                foundItemId = claim.itemId,   // ClaimRequest now uses itemId (was foundItemId)
                lostItemId = ""               // lostItemId removed from ClaimRequest schema
            )
            if (res.isSuccess) {
                _isResolvedSuccess.value = true
            }
            _isResolving.value = false
        }
    }
}
