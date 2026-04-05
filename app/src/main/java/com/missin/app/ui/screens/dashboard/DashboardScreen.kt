package com.missin.app.ui.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import android.Manifest
import android.os.Build
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missin.app.data.model.FoundItem
import com.missin.app.data.model.LostItem
import com.missin.app.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    onNavigateToReport: (Boolean) -> Unit, // true for Found, false for Lost
    onEditProfile: () -> Unit,
    onNavigateToClaim: (String, String) -> Unit,
    onNavigateToIncomingClaims: () -> Unit,
    onNavigateToYourReports: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(
            Manifest.permission.POST_NOTIFICATIONS
        )
        
        LaunchedEffect(notificationPermissionState.status) {
            if (!notificationPermissionState.status.isGranted) {
                val snackBarResult = snackbarHostState.showSnackbar(
                    message = "We need notification permission to alert you when a match is found for your item.",
                    actionLabel = "Allow",
                    duration = SnackbarDuration.Indefinite
                )
                if (snackBarResult == SnackbarResult.ActionPerformed) {
                    notificationPermissionState.launchPermissionRequest()
                }
            }
        }
    }

    val userProfile by viewModel.userProfile.collectAsState()
    val recentLost by viewModel.recentLostItems.collectAsState()
    val recentFound by viewModel.recentFoundItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Lost, 1: Found

    val haptic = LocalHapticFeedback.current

    val deepNavy = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val electricBlue = MaterialTheme.colorScheme.primary

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = deepNavy,
        topBar = {
            // Profile Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Hello, ${userProfile?.name?.substringBefore(" ") ?: "User"}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "KarmaPoints ",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )
                            Text(
                                text = "${userProfile?.karma ?: 0L}",
                                style = MaterialTheme.typography.bodySmall,
                                color = com.missin.app.ui.theme.MutedEmerald
                            )
                        }
                    }
                    Row {
                        IconButton(
                            onClick = { viewModel.loadRecentFeeds() },
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                                .padding(4.dp)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = textColor)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                                .padding(4.dp)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = textColor)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Primary Action Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ActionCard(
                    title = "Report Lost\nItem",
                    icon = Icons.Filled.Search,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = com.missin.app.ui.theme.BoneWhite,
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    onClick = { onNavigateToReport(false) }
                )
                ActionCard(
                    title = "Report Found\nItem",
                    icon = Icons.Filled.Add,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = com.missin.app.ui.theme.MutedEmerald,
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    onClick = { onNavigateToReport(true) }
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onNavigateToIncomingClaims,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = com.missin.app.ui.theme.MutedEmerald
                    ),
                    border = BorderStroke(1.dp, com.missin.app.ui.theme.MutedEmerald),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Filled.List, contentDescription = null, tint = com.missin.app.ui.theme.MutedEmerald)
                    Spacer(Modifier.width(4.dp))
                    Text("Claims", color = com.missin.app.ui.theme.MutedEmerald, fontWeight = FontWeight.Medium)
                }
                
                OutlinedButton(
                    onClick = onNavigateToYourReports,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = com.missin.app.ui.theme.MutedEmerald
                    ),
                    border = BorderStroke(1.dp, com.missin.app.ui.theme.MutedEmerald),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Filled.MenuBook, contentDescription = null, tint = com.missin.app.ui.theme.MutedEmerald)
                    Spacer(Modifier.width(4.dp))
                    Text("Your Reports", color = com.missin.app.ui.theme.MutedEmerald, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Dynamic Match Feed
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = textColor,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = com.missin.app.ui.theme.MutedEmerald,
                        height = 1.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Recent Lost", color = if (selectedTab == 0) textColor else textColor.copy(alpha = 0.6f)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Recent Found", color = if (selectedTab == 1) textColor else textColor.copy(alpha = 0.6f)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val currentFeed = if (selectedTab == 0) recentLost else recentFound

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = electricBlue)
                }
            } else if (currentFeed.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Expanding search radius...",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(items = currentFeed, key = { item -> if (item is LostItem) item.id else (item as FoundItem).id }) { item ->
                        MatchCard(
                            item = item,
                            score = 0.0,
                            onClaimClick = if (item is com.missin.app.data.model.FoundItem) { { onNavigateToClaim(item.id, item.userId) } } else null
                        )
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFF333333))
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false },
            onLogout = {
                showSettings = false
                viewModel.logout()
                onLogout()
            },
            onEditProfile = onEditProfile,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme
        )
    }
}

@Composable
fun ActionCard(title: String, icon: ImageVector, containerColor: Color, contentColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, animationSpec = androidx.compose.animation.core.tween(100))
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = contentColor)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF333333)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = contentColor, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = com.missin.app.ui.theme.BoneWhite,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MatchCard(
    item: Any, 
    score: Double,
    onClaimClick: (() -> Unit)? = null
) {
    val category = if (item is LostItem) item.category else (item as FoundItem).category
    val timestamp = if (item is LostItem) item.timestamp else (item as FoundItem).timestamp
    val itemType = if (item is LostItem) item.itemType else (item as FoundItem).itemType
    val title = if (item is LostItem) item.title.ifEmpty { category.ifEmpty { "Unknown Item" } } else (item as FoundItem).title.ifEmpty { category.ifEmpty { "Unknown Item" } }
    val imageUrl = if (item is LostItem) item.imageUrl else (item as FoundItem).imageUrl
    val location = if (item is LostItem) item.location else (item as FoundItem).location
    
    var showImageDialog by remember { mutableStateOf(false) }
    val isLost = itemType.equals("lost", ignoreCase = true)
    val iconTint = if (isLost) Color(0xFFE53935) else Color(0xFF43A047)
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val timeElapsed = remember(timestamp) {
        val diff = System.currentTimeMillis() - timestamp
        val hours = diff / (1000 * 60 * 60)
        val minutes = diff / (1000 * 60)
        when {
            hours > 24 -> "${hours / 24}d ago"
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, animationSpec = androidx.compose.animation.core.tween(100))
    val haptic = LocalHapticFeedback.current

    val textColor = com.missin.app.ui.theme.BoneWhite
    val matteCharcoal = com.missin.app.ui.theme.MatteCharcoal
    val electricBlue = com.missin.app.ui.theme.MutedEmerald
    val borderDark = Color(0xFF333333)

    fun getCategoryIcon(cat: String): ImageVector {
        return when (cat) {
            "Wallet/Purse" -> Icons.Default.AccountBalanceWallet
            "Mobile/Electronics" -> Icons.Default.PhoneAndroid
            "Keys" -> Icons.Default.VpnKey
            "Bag/Backpack" -> Icons.Default.Backpack
            "Documents/ID" -> Icons.Default.Assignment
            "Jewelry/Watch" -> Icons.Default.Watch
            "Clothing" -> Icons.Default.Checkroom
            "Pets" -> Icons.Default.Pets
            else -> Icons.Default.Pending
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = textColor)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
        colors = CardDefaults.cardColors(containerColor = matteCharcoal),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, borderDark, RoundedCornerShape(12.dp))
                    .background(matteCharcoal),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    getCategoryIcon(category),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isLost) "LOST" else "FOUND",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = textColor,
                        modifier = Modifier
                            .background(
                                color = iconTint,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = textColor,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (isLost) {
                    if (imageUrl.isBlank()) {
                        Text(
                            text = "No image available",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Color.Gray
                        )
                    } else {
                        Text(
                            text = "View Image",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = electricBlue,
                            modifier = Modifier.clickable { showImageDialog = true }
                        )
                    }
                } else {
                    Text(
                        text = "View on map",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = electricBlue,
                        modifier = Modifier.clickable {
                            location?.let {
                                val uri = android.net.Uri.parse("geo:${it.latitude},${it.longitude}?q=${it.latitude},${it.longitude}(Found Location)")
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "No map app found", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } ?: android.widget.Toast.makeText(context, "Location not available", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = timeElapsed,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.LightGray
                )
                if (item is com.missin.app.data.model.FoundItem && onClaimClick != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onClaimClick,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                        border = BorderStroke(1.dp, textColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text("Claim", color = textColor, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    if (showImageDialog && imageUrl.isNotBlank()) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showImageDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                coil.compose.AsyncImage(
                    model = imageUrl,
                    contentDescription = "Item Image",
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                IconButton(
                    onClick = { showImageDialog = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(0.5f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Filled.Close, "Close", tint = Color.White)
                }
            }
        }
    }
}
