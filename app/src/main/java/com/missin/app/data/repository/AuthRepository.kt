package com.missin.app.data.repository

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.missin.app.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthState {
    object Idle : AuthState()
    object SendingOTP : AuthState()
    data class CodeSent(val verificationId: String) : AuthState()
    object Verifying : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun sendOTP(phoneNumber: String, activity: Activity) {
        _authState.value = AuthState.SendingOTP

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-retrieval condition
                signInWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Verification failed")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                _authState.value = AuthState.CodeSent(verificationId)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
            
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOTP(verificationId: String, code: String) {
        _authState.value = AuthState.Verifying
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithCredential(credential)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    if (firebaseUser != null) {
                        checkAndCreateUserProfile(firebaseUser.uid, firebaseUser.phoneNumber ?: "")
                    } else {
                        _authState.value = AuthState.Error("Unknown authentication error")
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.localizedMessage ?: "Sign in failed")
                }
            }
    }

    private fun checkAndCreateUserProfile(uid: String, phoneNumber: String) {
        val userRef = firestore.collection("users").document(uid)
        userRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val newUser = User(uid = uid, phoneNumber = phoneNumber)
                userRef.set(newUser).addOnSuccessListener {
                    _authState.value = AuthState.Success(newUser)
                }.addOnFailureListener {
                    _authState.value = AuthState.Error("Failed to create user profile")
                }
            } else {
                val existingUser = snapshot.toObject(User::class.java)
                if (existingUser != null) {
                    _authState.value = AuthState.Success(existingUser)
                } else {
                    _authState.value = AuthState.Error("Failed to parse user profile")
                }
            }
        }.addOnFailureListener {
            _authState.value = AuthState.Error("Failed to fetch user profile")
        }
    }
    
    fun updateProfile(name: String, email: String?, onComplete: (Boolean, String?) -> Unit) {
        val uid = getCurrentUserUid()
        if (uid == null) {
            onComplete(false, "User not authenticated")
            return
        }
        
        val userRef = firestore.collection("users").document(uid)
        val updates = mutableMapOf<String, Any>("name" to name)
        if (email != null) {
            updates["email"] = email
        }
        
        userRef.update(updates).addOnSuccessListener {
            onComplete(true, null)
        }.addOnFailureListener {
            onComplete(false, it.localizedMessage ?: "Failed to update profile")
        }
    }
    
    fun getCurrentUserUid(): String? = auth.currentUser?.uid
}
