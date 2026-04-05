package com.missin.app.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.missin.app.data.repository.AuthRepository
import com.missin.app.data.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    fun sendOTP(phoneNumber: String, activity: Activity) {
        authRepository.sendOTP(phoneNumber, activity)
    }

    fun verifyOTP(verificationId: String, code: String) {
        authRepository.verifyOTP(verificationId, code)
    }

    fun resetState() {
        authRepository.resetState()
    }

    fun updateProfile(name: String, email: String?, onComplete: (Boolean, String?) -> Unit) {
        authRepository.updateProfile(name, email, onComplete)
    }
}
