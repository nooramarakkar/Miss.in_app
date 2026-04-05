package com.missin.app.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missin.app.data.model.FoundItem
import com.missin.app.data.model.LostItem
import com.missin.app.ui.theme.BoneWhite
import com.missin.app.ui.viewmodel.YourReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourReportsScreen(
    onBack: () -> Unit,
    viewModel: YourReportsViewModel = hiltViewModel()
) {
    val reports by viewModel.userReports.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val deepNavy = MaterialTheme.colorScheme.background

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text("Your Reports", color = BoneWhite, fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = BoneWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchUserReports() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = BoneWhite)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = deepNavy)
            )
        },
        containerColor = deepNavy
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "Error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                reports.isEmpty() -> {
                    Text(
                        text = "No reports yet.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                    ) {
                        items(
                            items = reports, 
                            key = { item -> if (item is LostItem) item.id else (item as FoundItem).id }
                        ) { item ->
                            MatchCard(
                                item = item,
                                score = 0.0,
                                onClaimClick = null // Claims are for incoming matches, not self reports
                            )
                        }
                    }
                }
            }
        }
    }
}
