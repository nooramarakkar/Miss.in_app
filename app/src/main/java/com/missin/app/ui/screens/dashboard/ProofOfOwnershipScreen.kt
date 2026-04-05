package com.missin.app.ui.screens.dashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missin.app.ui.viewmodel.ProofOfOwnershipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProofOfOwnershipScreen(
    foundItemId: String,
    finderId: String,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: ProofOfOwnershipViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    val haptic = LocalHapticFeedback.current
    val deepNavy = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val electricBlue = Color(0xFF1E5A8A)

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    LaunchedEffect(isSuccess) {
        if (isSuccess) onComplete()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proof of Ownership", color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = deepNavy)
            )
        },
        containerColor = deepNavy
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Text(
                "Describe Unique Marks",
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description of Contents/Unique Marks", color = textColor.copy(alpha = 0.7f)) },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedBorderColor = electricBlue,
                    unfocusedBorderColor = textColor.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { imagePicker.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (imageUri != null) "Change Proof Image" else "Attach Proof Image (Optional)", color = textColor)
            }

            if (imageUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp))) {
                    coil.compose.AsyncImage(
                        model = imageUri,
                        contentDescription = "Proof Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    IconButton(
                        onClick = { imageUri = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(0.5f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Filled.Close, "Remove", tint = Color.White)
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.weight(1f))

            val context = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.submitClaim(foundItemId, finderId, description, imageUri, context)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading && description.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = electricBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Submit Claim", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
