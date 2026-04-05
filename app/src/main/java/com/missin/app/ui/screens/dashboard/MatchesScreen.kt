package com.missin.app.ui.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missin.app.data.model.FoundItem
import com.missin.app.data.model.LostItem
import com.missin.app.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    onNavigateToMatchDetail: (theirLat: Double, theirLng: Double, myLat: Double, myLng: Double) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val matches by viewModel.potentialMatches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val boneWhite = com.missin.app.ui.theme.BoneWhite
    val matteCharcoal = com.missin.app.ui.theme.MatteCharcoal

    Column(modifier = Modifier.fillMaxSize().background(matteCharcoal)) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Potential Matches",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = boneWhite
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = matteCharcoal)
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = boneWhite)
            }
        } else if (matches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No matches found right now.\nWe will securely notify you if a match occurs.",
                    color = boneWhite.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(matches) { match ->
                    val category = if (match is FoundItem) match.category else (match as LostItem).category
                    val theirLat = (if (match is FoundItem) match.location?.latitude else (match as LostItem).location?.latitude) ?: 0.0
                    val theirLng = (if (match is FoundItem) match.location?.longitude else (match as LostItem).location?.longitude) ?: 0.0
                    
                    // We don't have our own location strictly bound to the `matches` query locally right now, 
                    // so we supply a placeholder `myLat` scaling logic unless the match embeds it. 
                    val myLat = 37.4221
                    val myLng = -122.0841
                    // Calculates distance using standard Haversine logic
                    val distance = remember(theirLat, theirLng, myLat, myLng) {
                        val r = 6371.0 // Radius of Earth in km
                        val latDistance = Math.toRadians(myLat - theirLat)
                        val lngDistance = Math.toRadians(myLng - theirLng)
                        val a = kotlin.math.sin(latDistance / 2) * kotlin.math.sin(latDistance / 2) +
                                kotlin.math.cos(Math.toRadians(theirLat)) * kotlin.math.cos(Math.toRadians(myLat)) *
                                kotlin.math.sin(lngDistance / 2) * kotlin.math.sin(lngDistance / 2)
                        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
                        r * c
                    }
                    val distanceText = String.format("Distance: ~%.1f km", distance)

                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, animationSpec = tween(100))
                    val haptic = LocalHapticFeedback.current

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigateToMatchDetail(theirLat, theirLng, myLat, myLng)
                            },
                        colors = CardDefaults.cardColors(containerColor = matteCharcoal),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF333333)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.ifEmpty { "Unknown Category" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = boneWhite
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = distanceText,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = com.missin.app.ui.theme.MutedEmerald
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(com.missin.app.ui.theme.MutedEmerald)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Pending", color = boneWhite, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
