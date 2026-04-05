package com.missin.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.missin.app.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onProfileComplete: () -> Unit,
    isEdit: Boolean = false,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val deepNavy = com.missin.app.ui.theme.MatteCharcoal
    val textColor = com.missin.app.ui.theme.BoneWhite
    val electricBlue = com.missin.app.ui.theme.MutedEmerald

    LaunchedEffect(isEdit) {
        if (isEdit) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        name = doc.getString("name") ?: ""
                        email = doc.getString("email") ?: ""
                    }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = deepNavy
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isEdit) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalArrangement = Arrangement.Start) {
                    IconButton(onClick = onProfileComplete) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                }
            }

            Text(
                text = if (isEdit) "Edit Profile" else "Complete Profile",
                style = MaterialTheme.typography.displaySmall,
                color = textColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = if (isEdit) "Update your account details below." else "Just a few final details before you begin.",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name", color = textColor.copy(alpha = 0.7f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = deepNavy,
                    unfocusedContainerColor = deepNavy,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedBorderColor = textColor,
                    unfocusedBorderColor = textColor.copy(alpha = 0.5f)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email (Optional)", color = textColor.copy(alpha = 0.7f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = deepNavy,
                    unfocusedContainerColor = deepNavy,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedBorderColor = textColor,
                    unfocusedBorderColor = textColor.copy(alpha = 0.5f)
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { 
                    if (name.isNotBlank()) {
                        isLoading = true
                        viewModel.updateProfile(name, email.takeIf { it.isNotBlank() } ) { success, errMsg ->
                            isLoading = false
                            if (success) {
                                onProfileComplete()
                            } else {
                                error = errMsg ?: "Failed to update profile"
                            }
                        }
                    } else {
                        error = "Name is required"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = electricBlue, contentColor = textColor)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = textColor, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (isEdit) "Save Changes" else "Complete Setup", style = MaterialTheme.typography.titleMedium, color = textColor)
                }
            }
            
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
