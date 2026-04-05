package com.missin.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.missin.app.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YourReportsViewModel @Inject constructor(
    private val repository: ItemRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _userReports = MutableStateFlow<List<Any>>(emptyList())
    val userReports: StateFlow<List<Any>> = _userReports.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        fetchUserReports()
    }

    fun fetchUserReports() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _errorMessage.value = "User not logged in."
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.getUserReports(currentUser.uid).fold(
                onSuccess = { reports ->
                    _userReports.value = reports
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message ?: "Failed to fetch reports."
                }
            )
            _isLoading.value = false
        }
    }
}
