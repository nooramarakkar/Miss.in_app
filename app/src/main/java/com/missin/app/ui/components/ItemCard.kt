package com.missin.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ItemCard(
    title: String,
    description: String,
    imageUrl: String?,
    actionContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = com.missin.app.ui.theme.MatteCharcoal),
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF333333))
    ) {
        Column {
            if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Item Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = com.missin.app.ui.theme.BoneWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📍 Map Location",
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.missin.app.ui.theme.MutedEmerald
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.missin.app.ui.theme.BoneWhite.copy(alpha = 0.7f)
                )

                if (actionContent != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    actionContent()
                }
            }
        }
    }
}
