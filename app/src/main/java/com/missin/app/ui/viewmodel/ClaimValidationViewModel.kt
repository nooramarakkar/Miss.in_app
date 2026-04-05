package com.missin.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.missin.app.data.model.ClaimRequest
import com.missin.app.data.repository.ClaimRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClaimValidationViewModel @Inject constructor(
    private val claimRepository: ClaimRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _incomingClaims = MutableStateFlow<List<ClaimRequest>>(emptyList())
    val incomingClaims = _incomingClaims.asStateFlow()

    private val _outgoingClaims = MutableStateFlow<List<ClaimRequest>>(emptyList())
    val outgoingClaims = _outgoingClaims.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadAllClaims() {
        viewModelScope.launch {
            _isLoading.value = true
            val uid = auth.currentUser?.uid
            if (uid != null) {
                val incomingResult = claimRepository.getIncomingClaims(uid)
                if (incomingResult.isSuccess) {
                    _incomingClaims.value = incomingResult.getOrNull() ?: emptyList()
                }
                
                val outgoingResult = claimRepository.getOutgoingClaims(uid)
                if (outgoingResult.isSuccess) {
                    _outgoingClaims.value = outgoingResult.getOrNull() ?: emptyList()
                }
            }
            _isLoading.value = false
        }
    }

    fun declineClaim(claimId: String) {
        viewModelScope.launch {
            val result = claimRepository.updateClaimStatus(claimId, "declined")
            if (result.isSuccess) {
                _incomingClaims.value = _incomingClaims.value.filter { it.id != claimId }
                _outgoingClaims.value = _outgoingClaims.value.filter { it.id != claimId }
            }
        }
    }

    fun verifyClaim(claimId: String, onVerified: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = claimRepository.updateClaimStatus(claimId, "active")
            if (result.isSuccess) {
                onVerified()
            }
            _isLoading.value = false
        }
    }
}
