package com.missin.app.ui.screens.auth

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missin.app.R
import com.missin.app.data.repository.AuthState
import com.missin.app.ui.viewmodel.AuthViewModel
import org.json.JSONArray

data class Country(val name: String, val code: String, val flag: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onProfileSetupNeeded: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    // Parse countries
    val countries = remember {
        val jsonString = context.assets.open("countries.json").bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonString)
        val list = mutableListOf<Country>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(Country(obj.getString("name"), obj.getString("code"), obj.getString("flag")))
        }
        list
    }

    var selectedCountry by remember { mutableStateOf(countries.firstOrNull { it.code == "+91" } ?: countries.first()) }
    var phoneNumber by remember { mutableStateOf("") }
    var optCode by remember { mutableStateOf("") }
    var expandedCountry by remember { mutableStateOf(false) }

    // Palette Colors
    val matteCharcoal = com.missin.app.ui.theme.MatteCharcoal
    val boneWhite = com.missin.app.ui.theme.BoneWhite

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = matteCharcoal
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.bg_miss_in_blurred),
                contentDescription = "Background",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "miss.in",
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    color = boneWhite,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Welcome to miss.in | Reuniting you with what matters.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 48.dp),
                    textAlign = TextAlign.Center
                )

            when (authState) {
                is AuthState.Idle, is AuthState.Error -> {
                    // Dual-Box Login
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Country Selector
                        Box(modifier = Modifier.weight(0.35f)) {
                            OutlinedTextField(
                                value = "${selectedCountry.flag} ${selectedCountry.code}",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedCountry = true },
                                enabled = false,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = boneWhite),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = boneWhite,
                                    disabledContainerColor = matteCharcoal,
                                    disabledTextColor = boneWhite
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            DropdownMenu(
                                expanded = expandedCountry,
                                onDismissRequest = { expandedCountry = false },
                                modifier = Modifier.background(matteCharcoal)
                            ) {
                                countries.forEach { country ->
                                    DropdownMenuItem(
                                        text = { Text("${country.flag} ${country.name} (${country.code})", color = boneWhite) },
                                        onClick = {
                                            selectedCountry = country
                                            expandedCountry = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Phone Input
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { if (it.length <= 15) phoneNumber = it },
                            placeholder = { Text("Phone Number", color = boneWhite) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.65f),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = boneWhite),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = boneWhite,
                                unfocusedBorderColor = boneWhite,
                                focusedContainerColor = matteCharcoal,
                                unfocusedContainerColor = matteCharcoal,
                                cursorColor = boneWhite
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.95f else 1f,
                        animationSpec = tween(100)
                    )

                    OutlinedButton(
                        onClick = { viewModel.sendOTP(selectedCountry.code + phoneNumber, context as Activity) },
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(scale),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = boneWhite),
                        border = BorderStroke(1.dp, boneWhite)
                    ) {
                        Text("Send Code", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                    }

                    if (authState is AuthState.Error) {
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is AuthState.CodeSent -> {
                    val verificationId = (authState as AuthState.CodeSent).verificationId
                    OutlinedTextField(
                        value = optCode,
                        onValueChange = { optCode = it },
                        label = { Text("OTP Code", color = boneWhite) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = boneWhite),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = boneWhite,
                            unfocusedBorderColor = boneWhite,
                            focusedContainerColor = matteCharcoal,
                            unfocusedContainerColor = matteCharcoal,
                            cursorColor = boneWhite
                        )
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.95f else 1f,
                        animationSpec = tween(100)
                    )

                    OutlinedButton(
                        onClick = { viewModel.verifyOTP(verificationId, optCode) },
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(scale),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = boneWhite),
                        border = BorderStroke(1.dp, boneWhite)
                    ) {
                        Text("Verify & Login", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                    }
                }
                is AuthState.Verifying, is AuthState.SendingOTP -> {
                    CircularProgressIndicator(color = boneWhite)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Securely verifying...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black
                    )
                }
                is AuthState.Success -> {
                    val user = (authState as AuthState.Success).user
                    LaunchedEffect(Unit) {
                        if (user.name.isBlank()) {
                            onProfileSetupNeeded()
                        } else {
                            onAuthSuccess()
                        }
                    }
                }
            }
        }
        }
    }
}
