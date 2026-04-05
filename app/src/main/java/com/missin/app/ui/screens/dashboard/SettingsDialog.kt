package com.missin.app.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = textColor,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Theme Toggle removed (Universal Matte Identity)

            // Edit Profile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clickable {
                        onDismiss()
                        onEditProfile()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Edit Profile", style = MaterialTheme.typography.titleMedium, color = textColor)
                Icon(Icons.Filled.Person, contentDescription = "Edit Profile", tint = textColor)
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = com.missin.app.ui.theme.BoneWhite 
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.missin.app.ui.theme.BoneWhite)
            ) {
                Text("Logout", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
