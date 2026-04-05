package com.missin.app.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missin.app.data.model.ClaimRequest
import com.missin.app.ui.viewmodel.ClaimValidationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimValidationScreen(
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: ClaimValidationViewModel = hiltViewModel()
) {
    val incomingClaims by viewModel.incomingClaims.collectAsState()
    val outgoingClaims by viewModel.outgoingClaims.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    val deepNavy = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val electricBlue = Color(0xFF1E5A8A)

    LaunchedEffect(Unit) {
        viewModel.loadAllClaims()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Claims", color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAllClaims() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = textColor)
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
                .padding(horizontal = 16.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = textColor,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = electricBlue,
                        height = 1.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Incoming", color = if (selectedTab == 0) textColor else textColor.copy(alpha = 0.6f)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Outgoing", color = if (selectedTab == 1) textColor else textColor.copy(alpha = 0.6f)) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            val currentClaims = if (selectedTab == 0) incomingClaims else outgoingClaims

            if (isLoading && currentClaims.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = electricBlue)
                }
            } else if (currentClaims.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No claims here.", color = textColor.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(currentClaims) { claim ->
                        ClaimCard(
                            claim = claim,
                            textColor = textColor,
                            electricBlue = electricBlue,
                            isOutgoing = selectedTab == 1,
                            onDecline = { viewModel.declineClaim(claim.id) },
                            onVerify = {
                                viewModel.verifyClaim(claim.id) {
                                    onNavigateToChat(claim.id)
                                }
                            },
                            onChat = { onNavigateToChat(claim.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClaimCard(
    claim: ClaimRequest,
    textColor: Color,
    electricBlue: Color,
    isOutgoing: Boolean,
    onDecline: () -> Unit,
    onVerify: () -> Unit,
    onChat: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Proof Description:", style = MaterialTheme.typography.titleSmall, color = textColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(claim.proofDescription, style = MaterialTheme.typography.bodyMedium, color = textColor.copy(alpha = 0.8f))
            
            if (claim.proofImageUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Image Attached", color = electricBlue, style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (isOutgoing) {
                    if (claim.status == "active" || claim.status == "resolved") {
                        Button(
                            onClick = onChat,
                            colors = ButtonDefaults.buttonColors(containerColor = electricBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Open Chat", color = Color.White)
                        }
                    } else {
                        Text("Status: ${claim.status}", color = electricBlue, style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    if (claim.status == "pending") {
                        TextButton(onClick = onDecline) {
                            Text("Decline", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onVerify,
                            colors = ButtonDefaults.buttonColors(containerColor = electricBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Verify & Chat", color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = onChat,
                            colors = ButtonDefaults.buttonColors(containerColor = electricBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Open Chat", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
